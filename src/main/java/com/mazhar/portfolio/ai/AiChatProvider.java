package com.mazhar.portfolio.ai;

import java.util.List;

/**
 * Turns a conversation into the assistant's next reply. The only implementation that talks to a
 * model is {@link AnthropicChatProvider}; keeping this interface means the service and controller
 * never depend on a vendor SDK, and tests can use a plain fake.
 */
public interface AiChatProvider {

    /** @param turns oldest first; the last one is the visitor's new question */
    String reply(List<ChatTurn> turns);
}
