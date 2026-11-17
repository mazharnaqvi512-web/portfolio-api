package com.mazhar.portfolio.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.mazhar.portfolio.exception.AiDailyLimitException;
import com.mazhar.portfolio.exception.InvalidChatRequestException;

class AiChatServiceTest {

    private final List<List<ChatTurn>> received = new ArrayList<>();
    private final AiChatProvider provider = turns -> {
        received.add(turns);
        return "reply";
    };

    private static ChatTurn user(String text) {
        return new ChatTurn(ChatTurn.Role.USER, text);
    }

    private static ChatTurn assistant(String text) {
        return new ChatTurn(ChatTurn.Role.ASSISTANT, text);
    }

    private AiChatService service(int dailyCap) {
        return new AiChatService(provider, new DailyCap(dailyCap, Clock.fixed(Instant.parse("2026-11-04T10:00:00Z"), ZoneOffset.UTC)));
    }

    @Test
    void passesAValidConversationToTheProviderAndReturnsItsReply() {
        List<ChatTurn> turns = List.of(user("Hi"), assistant("Hello"), user("Skills?"));

        assertThat(service(5).chat(turns)).isEqualTo("reply");
        assertThat(received).containsExactly(turns);
    }

    @Test
    void rejectsAConversationThatDoesNotStartWithTheUser() {
        assertThatThrownBy(() -> service(5).chat(List.of(assistant("Hello"), user("Hi"))))
            .isInstanceOf(InvalidChatRequestException.class)
            .hasMessageContaining("start with a user message");
    }

    @Test
    void rejectsAConversationThatDoesNotEndWithTheUser() {
        assertThatThrownBy(() -> service(5).chat(List.of(user("Hi"), assistant("Hello"))))
            .isInstanceOf(InvalidChatRequestException.class)
            .hasMessageContaining("end with a user message");
    }

    @Test
    void rejectsAnEmptyConversation() {
        assertThatThrownBy(() -> service(5).chat(List.of())).isInstanceOf(InvalidChatRequestException.class);
    }

    @Test
    void stopsCallingTheProviderOnceTheDailyCapIsUsedUp() {
        AiChatService service = service(2);

        service.chat(List.of(user("1")));
        service.chat(List.of(user("2")));

        assertThatThrownBy(() -> service.chat(List.of(user("3")))).isInstanceOf(AiDailyLimitException.class);
        assertThat(received).hasSize(2);
    }

    @Test
    void anInvalidRequestDoesNotUseUpTheDailyBudget() {
        AiChatService service = service(1);

        assertThatThrownBy(() -> service.chat(List.of(assistant("x")))).isInstanceOf(InvalidChatRequestException.class);

        assertThat(service.chat(List.of(user("real question")))).isEqualTo("reply");
    }
}
