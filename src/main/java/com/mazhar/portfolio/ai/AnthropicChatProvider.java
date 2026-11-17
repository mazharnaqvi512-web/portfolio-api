package com.mazhar.portfolio.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.TextBlockParam;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mazhar.portfolio.exception.AiProviderException;

/**
 * Answers through the official Anthropic Java SDK. No tools, no web access: the model only sees
 * the system prompt (the portfolio's facts) and the visitor's conversation.
 *
 * <p>Nothing the visitor typed and nothing the model returned is logged — only the outcome of the
 * call — and SDK errors are mapped to {@link AiProviderException}, whose cause stays in the log.
 */
public class AnthropicChatProvider implements AiChatProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicChatProvider.class);

    static final String REFUSAL_REPLY =
        "I can't help with that request. You can ask me about Syed Mazhar's experience, projects or skills.";

    private final AnthropicClient client;
    private final String model;
    private final long maxTokens;
    private final OutputConfig.Effort effort;
    private final String systemPrompt;

    public AnthropicChatProvider(
        AnthropicClient client,
        String model,
        long maxTokens,
        OutputConfig.Effort effort,
        String systemPrompt
    ) {
        this.client = client;
        this.model = model;
        this.maxTokens = maxTokens;
        this.effort = effort;
        this.systemPrompt = systemPrompt;
    }

    @Override
    public String reply(List<ChatTurn> turns) {
        MessageCreateParams.Builder request = MessageCreateParams.builder()
            .model(model)
            .maxTokens(maxTokens)
            // Identical on every request, so the provider can serve it from its prompt cache.
            .systemOfTextBlockParams(List.of(TextBlockParam.builder()
                .text(systemPrompt)
                .cacheControl(CacheControlEphemeral.builder().build())
                .build()))
            // Short factual answers: low effort keeps latency and cost down.
            .outputConfig(OutputConfig.builder().effort(effort).build());

        for (ChatTurn turn : turns) {
            if (turn.role() == ChatTurn.Role.USER) {
                request.addUserMessage(turn.content());
            } else {
                request.addAssistantMessage(turn.content());
            }
        }

        Message response;
        try {
            response = client.messages().create(request.build());
        } catch (AnthropicServiceException e) {
            log.warn("Anthropic API returned an error: status={} type={}", e.statusCode(), e.errorType().orElse(null));
            throw new AiProviderException("Anthropic API error", e);
        } catch (AnthropicException e) {
            log.warn("Anthropic call failed: {}", e.getClass().getSimpleName());
            throw new AiProviderException("Anthropic call failed", e);
        }

        if (response.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
            log.info("Anthropic declined a chat request (stop_reason=refusal)");
            return REFUSAL_REPLY;
        }

        String text = response.content().stream()
            .flatMap(block -> block.text().stream())
            .map(TextBlock::text)
            .collect(Collectors.joining("\n"))
            .strip();
        if (text.isEmpty()) {
            log.warn("Anthropic returned no text: stop_reason={}", response.stopReason().orElse(null));
            throw new AiProviderException("Anthropic returned an empty reply");
        }
        return text;
    }
}
