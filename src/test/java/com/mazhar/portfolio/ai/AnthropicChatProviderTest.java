package com.mazhar.portfolio.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.OutputConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.mazhar.portfolio.exception.AiProviderException;

/**
 * Runs the real SDK against a local stand-in for the Anthropic API, so the tests see exactly what
 * this application sends — without a network call, an API key or any cost.
 */
class AnthropicChatProviderTest {

    private static final String SYSTEM_PROMPT = "You are a test assistant. FACTS: the sky is blue.";
    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer server;
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private final AtomicReference<String> requestApiKey = new AtomicReference<>();
    private final AtomicReference<JsonNode> requestBody = new AtomicReference<>();
    private volatile int responseStatus;
    private volatile String responseBody;

    @BeforeEach
    void startFakeApi() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            requestApiKey.set(exchange.getRequestHeaders().getFirst("x-api-key"));
            requestBody.set(JSON.readTree(exchange.getRequestBody().readAllBytes()));
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("content-type", "application/json");
            exchange.sendResponseHeaders(responseStatus, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        respondWithText("Hello from the fake API.");
    }

    @AfterEach
    void stopFakeApi() {
        server.stop(0);
    }

    private AnthropicChatProvider provider() {
        AnthropicClient client = AnthropicOkHttpClient.builder()
            .apiKey("test-key")
            .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
            .maxRetries(0)
            .build();
        return new AnthropicChatProvider(client, "claude-opus-5-5", 2048, OutputConfig.Effort.LOW, SYSTEM_PROMPT);
    }

    private void respond(int status, String contentJson, String stopReason) {
        responseStatus = status;
        responseBody = """
            {"id":"msg_test","type":"message","role":"assistant","model":"claude-opus-5-5",
             "content":%s,"stop_reason":%s,"stop_sequence":null,
             "usage":{"input_tokens":10,"output_tokens":5}}
            """.formatted(contentJson, stopReason == null ? "null" : "\"" + stopReason + "\"");
    }

    private void respondWithText(String text) {
        respond(200, "[{\"type\":\"text\",\"text\":\"" + text + "\"}]", "end_turn");
    }

    private void respondWithError(int status, String errorType) {
        responseStatus = status;
        responseBody = "{\"type\":\"error\",\"error\":{\"type\":\"" + errorType + "\",\"message\":\"nope\"}}";
    }

    @Test
    void returnsTheModelsText() {
        String reply = provider().reply(List.of(new ChatTurn(ChatTurn.Role.USER, "Hi")));

        assertThat(reply).isEqualTo("Hello from the fake API.");
    }

    @Test
    void sendsTheConfiguredModelEffortLimitsAndTheApiKey() {
        provider().reply(List.of(new ChatTurn(ChatTurn.Role.USER, "Hi")));

        JsonNode body = requestBody.get();
        assertThat(requestPath.get()).isEqualTo("/v1/messages");
        assertThat(requestApiKey.get()).isEqualTo("test-key");
        assertThat(body.get("model").asText()).isEqualTo("claude-opus-5-5");
        assertThat(body.get("max_tokens").asLong()).isEqualTo(2048);
        assertThat(body.get("output_config").get("effort").asText()).isEqualTo("low");
    }

    @Test
    void sendsThePortfolioPromptAsACacheableSystemBlock() {
        provider().reply(List.of(new ChatTurn(ChatTurn.Role.USER, "Hi")));

        JsonNode system = requestBody.get().get("system");
        assertThat(system.isArray()).isTrue();
        assertThat(system.get(0).get("text").asText()).isEqualTo(SYSTEM_PROMPT);
        assertThat(system.get(0).get("cache_control").get("type").asText()).isEqualTo("ephemeral");
    }

    @Test
    void sendsTheConversationInOrderWithItsRoles() {
        provider().reply(List.of(
            new ChatTurn(ChatTurn.Role.USER, "What does he do?"),
            new ChatTurn(ChatTurn.Role.ASSISTANT, "He builds Angular apps."),
            new ChatTurn(ChatTurn.Role.USER, "Which ones?")));

        JsonNode messages = requestBody.get().get("messages");
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).get("role").asText()).isEqualTo("user");
        assertThat(messages.get(1).get("role").asText()).isEqualTo("assistant");
        assertThat(messages.get(2).get("role").asText()).isEqualTo("user");
        assertThat(messages.toString()).contains("What does he do?", "He builds Angular apps.", "Which ones?");
    }

    @Test
    void givesTheModelNoToolsAndNoSamplingOverrides() {
        provider().reply(List.of(new ChatTurn(ChatTurn.Role.USER, "Hi")));

        JsonNode body = requestBody.get();
        assertThat(body.has("tools")).isFalse();
        assertThat(body.has("temperature")).isFalse();
        assertThat(body.has("top_p")).isFalse();
        assertThat(body.has("top_k")).isFalse();
    }

    @Test
    void keepsOnlyTheTextBlocksOfAReplyThatAlsoHasThinking() {
        respond(200, "[{\"type\":\"thinking\",\"thinking\":\"\",\"signature\":\"sig\"},"
            + "{\"type\":\"text\",\"text\":\"The answer.\"}]", "end_turn");

        assertThat(provider().reply(List.of(new ChatTurn(ChatTurn.Role.USER, "Hi")))).isEqualTo("The answer.");
    }

    @Test
    void answersARefusalWithAFixedFriendlyReply() {
        respond(200, "[]", "refusal");

        assertThat(provider().reply(List.of(new ChatTurn(ChatTurn.Role.USER, "Hi"))))
            .isEqualTo(AnthropicChatProvider.REFUSAL_REPLY);
    }

    @Test
    void anEmptyReplyIsAProviderFailure() {
        respond(200, "[]", "end_turn");

        assertThatThrownBy(() -> provider().reply(List.of(new ChatTurn(ChatTurn.Role.USER, "Hi"))))
            .isInstanceOf(AiProviderException.class);
    }

    @Test
    void mapsAnApiErrorToAProviderFailureWithoutLeakingItsDetails() {
        respondWithError(429, "rate_limit_error");

        assertThatThrownBy(() -> provider().reply(List.of(new ChatTurn(ChatTurn.Role.USER, "Hi"))))
            .isInstanceOf(AiProviderException.class)
            .hasMessage("Anthropic API error");
    }

    @Test
    void mapsAServerErrorToAProviderFailure() {
        respondWithError(500, "api_error");

        assertThatThrownBy(() -> provider().reply(List.of(new ChatTurn(ChatTurn.Role.USER, "Hi"))))
            .isInstanceOf(AiProviderException.class);
    }

    @Test
    void mapsAnUnreachableApiToAProviderFailure() {
        AnthropicChatProvider provider = provider();
        server.stop(0);

        assertThatThrownBy(() -> provider.reply(List.of(new ChatTurn(ChatTurn.Role.USER, "Hi"))))
            .isInstanceOf(AiProviderException.class);
    }
}
