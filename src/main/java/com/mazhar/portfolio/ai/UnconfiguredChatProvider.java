package com.mazhar.portfolio.ai;

import java.util.List;
import com.mazhar.portfolio.exception.AiUnavailableException;

/**
 * Used when no API key is configured. The endpoint answers 503 rather than returning canned text
 * that would pass for a real model's answer.
 */
public class UnconfiguredChatProvider implements AiChatProvider {

    @Override
    public String reply(List<ChatTurn> turns) {
        throw new AiUnavailableException("The AI assistant is not configured on this server.");
    }
}
