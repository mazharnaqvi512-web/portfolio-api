package com.mazhar.portfolio.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anthropic.models.messages.OutputConfig;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.mazhar.portfolio.content.ContentLoader;
import com.mazhar.portfolio.exception.AiProviderException;

class AiConfigTest {

    private final PortfolioKnowledge knowledge = new PortfolioKnowledge(new ContentLoader());

    @Test
    void parsesEveryEffortLevelIgnoringCaseAndSpaces() {
        assertThat(AiConfig.parseEffort("low")).isEqualTo(OutputConfig.Effort.LOW);
        assertThat(AiConfig.parseEffort(" Medium ")).isEqualTo(OutputConfig.Effort.MEDIUM);
        assertThat(AiConfig.parseEffort("HIGH")).isEqualTo(OutputConfig.Effort.HIGH);
        assertThat(AiConfig.parseEffort("xhigh")).isEqualTo(OutputConfig.Effort.XHIGH);
        assertThat(AiConfig.parseEffort("max")).isEqualTo(OutputConfig.Effort.MAX);
    }

    @Test
    void parsingEffortDoesNotDependOnTheMachinesDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            // In a Turkish locale "MEDIUM".toLowerCase() is "medıum" (dotless i), which would match nothing.
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            assertThat(AiConfig.parseEffort("MEDIUM")).isEqualTo(OutputConfig.Effort.MEDIUM);
            assertThat(AiConfig.parseEffort("XHIGH")).isEqualTo(OutputConfig.Effort.XHIGH);
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void rejectsAnUnknownEffortAtStartupInsteadOfSendingItToTheApi() {
        assertThatThrownBy(() -> AiConfig.parseEffort("turbo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("turbo");
    }

    @Test
    void withoutAnApiKeyTheAssistantIsDisabledRatherThanFaked() {
        AiChatProvider provider = new AiConfig()
            .aiChatProvider(knowledge, "  ", "claude-opus-5-5", 4096, "low", Duration.ofSeconds(30), "", 1);

        assertThat(provider).isInstanceOf(UnconfiguredChatProvider.class);
    }

    @Test
    void withAnApiKeyItBuildsTheRealProvider() {
        AiChatProvider provider = new AiConfig()
            .aiChatProvider(knowledge, "sk-test-not-real", "claude-opus-5-5", 4096, "low", Duration.ofSeconds(30), "", 1);

        assertThat(provider).isInstanceOf(AnthropicChatProvider.class);
    }

    @Test
    void aBaseUrlOverrideStillBuildsTheRealProvider() {
        AiChatProvider provider = new AiConfig().aiChatProvider(
            knowledge, "sk-test-not-real", "claude-opus-5-5", 4096, "low", Duration.ofSeconds(30), "http://127.0.0.1:9", 1);

        assertThat(provider).isInstanceOf(AnthropicChatProvider.class);
    }

    @Test
    void aFailingCallIsRetriedExactlyAsOftenAsConfigured() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            byte[] body = "{\"type\":\"error\",\"error\":{\"type\":\"api_error\",\"message\":\"boom\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("content-type", "application/json");
            exchange.sendResponseHeaders(500, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            for (int retries : new int[] {0, 1}) {
                requests.set(0);
                AiChatProvider provider = new AiConfig().aiChatProvider(
                    knowledge, "sk-test-not-real", "claude-opus-5-5", 4096, "low", Duration.ofSeconds(5), baseUrl, retries);

                assertThatThrownBy(() -> provider.reply(List.of(new ChatTurn(ChatTurn.Role.USER, "Hi"))))
                    .isInstanceOf(AiProviderException.class);

                assertThat(requests.get()).as("requests with maxRetries=%d", retries).isEqualTo(retries + 1);
            }
        } finally {
            server.stop(0);
        }
    }
}
