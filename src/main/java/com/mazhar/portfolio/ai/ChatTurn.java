package com.mazhar.portfolio.ai;

/** One message of a conversation, independent of any AI vendor's wire format. */
public record ChatTurn(Role role, String content) {

    public enum Role {
        USER,
        ASSISTANT
    }
}
