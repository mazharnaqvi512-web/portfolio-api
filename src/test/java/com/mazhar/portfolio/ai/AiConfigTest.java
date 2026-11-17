package com.mazhar.portfolio.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anthropic.models.messages.OutputConfig;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import com.mazhar.portfolio.content.ContentLoader;

class AiConfigTest {

    @Test
    void parsesEveryEffortLevelIgnoringCaseAndSpaces() {
        assertThat(AiConfig.parseEffort("low")).isEqualTo(OutputConfig.Effort.LOW);
        assertThat(AiConfig.parseEffort(" Medium ")).isEqualTo(OutputConfig.Effort.MEDIUM);
        assertThat(AiConfig.parseEffort("HIGH")).isEqualTo(OutputConfig.Effort.HIGH);
        assertThat(AiConfig.parseEffort("xhigh")).isEqualTo(OutputConfig.Effort.XHIGH);
        assertThat(AiConfig.parseEffort("max")).isEqualTo(OutputConfig.Effort.MAX);
    }

    @Test
    void rejectsAnUnknownEffortAtStartupInsteadOfSendingItToTheApi() {
        assertThatThrownBy(() -> AiConfig.parseEffort("turbo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("turbo");
    }

    @Test
    void withoutAnApiKeyTheAssistantIsDisabledRatherThanFaked() {
        PortfolioKnowledge knowledge = new PortfolioKnowledge(new ContentLoader());

        AiChatProvider provider = new AiConfig()
            .aiChatProvider(knowledge, "  ", "claude-opus-5-5", 4096, "low", Duration.ofSeconds(30), "");

        assertThat(provider).isInstanceOf(UnconfiguredChatProvider.class);
    }

    @Test
    void withAnApiKeyItBuildsTheRealProvider() {
        PortfolioKnowledge knowledge = new PortfolioKnowledge(new ContentLoader());

        AiChatProvider provider = new AiConfig()
            .aiChatProvider(knowledge, "sk-test-not-real", "claude-opus-5-5", 4096, "low", Duration.ofSeconds(30), "");

        assertThat(provider).isInstanceOf(AnthropicChatProvider.class);
    }

    @Test
    void aBaseUrlOverrideStillBuildsTheRealProvider() {
        PortfolioKnowledge knowledge = new PortfolioKnowledge(new ContentLoader());

        AiChatProvider provider = new AiConfig().aiChatProvider(
            knowledge, "sk-test-not-real", "claude-opus-5-5", 4096, "low", Duration.ofSeconds(30), "http://127.0.0.1:9");

        assertThat(provider).isInstanceOf(AnthropicChatProvider.class);
    }
}
