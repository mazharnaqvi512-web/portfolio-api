package com.mazhar.portfolio.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.OutputConfig;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the assistant. With no API key it falls back to {@link UnconfiguredChatProvider} (the
 * endpoint answers 503); the key is only ever read from configuration, normally the
 * ANTHROPIC_API_KEY environment variable, and is never logged.
 */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    AiChatProvider aiChatProvider(
        PortfolioKnowledge knowledge,
        @Value("${ai.anthropic.api-key:}") String apiKey,
        @Value("${ai.anthropic.model:claude-opus-5-5}") String model,
        @Value("${ai.anthropic.max-tokens:4096}") long maxTokens,
        @Value("${ai.anthropic.effort:low}") String effort,
        @Value("${ai.anthropic.timeout:30s}") Duration timeout,
        @Value("${ai.anthropic.base-url:}") String baseUrl,
        @Value("${ai.anthropic.max-retries:1}") int maxRetries
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("AI assistant disabled: no Anthropic API key configured (POST /api/ai/chat answers 503)");
            return new UnconfiguredChatProvider();
        }
        // The SDK retries timeouts and 5xx on its own (default 2). Each retry can wait out the full timeout, so the
        // worst case is timeout x (maxRetries + 1) while the visitor waits and a request thread is held: keep it small.
        AnthropicOkHttpClient.Builder builder =
            AnthropicOkHttpClient.builder().apiKey(apiKey).timeout(timeout).maxRetries(maxRetries);
        // Only for pointing at a proxy or a local stand-in API (development, demos); empty means Anthropic's own endpoint.
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }
        AnthropicClient client = builder.build();
        log.info("AI assistant enabled: model={} effort={} maxTokens={} maxRetries={} baseUrl={}",
            model, effort, maxTokens, maxRetries, baseUrl == null || baseUrl.isBlank() ? "(default)" : baseUrl);
        return new AnthropicChatProvider(client, model, maxTokens, parseEffort(effort), knowledge.systemPrompt());
    }

    @Bean
    DailyCap aiDailyCap(@Value("${ai.daily-cap:100}") int dailyCap) {
        return new DailyCap(dailyCap, Clock.systemUTC());
    }

    static OutputConfig.Effort parseEffort(String value) {
        // Locale.ROOT: in a Turkish locale "MEDIUM".toLowerCase() is "medıum" (dotless i) and would match nothing.
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "low" -> OutputConfig.Effort.LOW;
            case "medium" -> OutputConfig.Effort.MEDIUM;
            case "high" -> OutputConfig.Effort.HIGH;
            case "xhigh" -> OutputConfig.Effort.XHIGH;
            case "max" -> OutputConfig.Effort.MAX;
            default -> throw new IllegalArgumentException(
                "ai.anthropic.effort must be one of low, medium, high, xhigh, max (was '" + value + "')");
        };
    }
}
