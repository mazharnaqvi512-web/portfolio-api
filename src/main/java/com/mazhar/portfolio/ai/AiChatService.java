package com.mazhar.portfolio.ai;

import java.util.List;
import org.springframework.stereotype.Service;
import com.mazhar.portfolio.exception.AiDailyLimitException;
import com.mazhar.portfolio.exception.InvalidChatRequestException;

@Service
public class AiChatService {

    private final AiChatProvider provider;
    private final DailyCap dailyCap;

    public AiChatService(AiChatProvider provider, DailyCap dailyCap) {
        this.provider = provider;
        this.dailyCap = dailyCap;
    }

    public String chat(List<ChatTurn> turns) {
        if (turns.isEmpty() || turns.get(0).role() != ChatTurn.Role.USER) {
            throw new InvalidChatRequestException("The conversation must start with a user message.");
        }
        if (turns.get(turns.size() - 1).role() != ChatTurn.Role.USER) {
            throw new InvalidChatRequestException("The conversation must end with a user message.");
        }
        // After validation, so a malformed request never uses up a call from the daily budget.
        if (!dailyCap.tryAcquire()) {
            throw new AiDailyLimitException();
        }
        return provider.reply(turns);
    }
}
