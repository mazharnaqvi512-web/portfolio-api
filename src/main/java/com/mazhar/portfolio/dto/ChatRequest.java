package com.mazhar.portfolio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * A visitor's conversation so far, oldest first, ending with their latest question.
 * The limits matter: this endpoint spends real money per call, and everything in it is
 * untrusted input (including "assistant" turns, which a caller can fabricate).
 */
public record ChatRequest(
    @NotEmpty @Size(max = 10, message = "must contain at most 10 messages") List<@Valid ChatMessage> messages
) {

    public record ChatMessage(
        @NotBlank @Pattern(regexp = "user|assistant", message = "must be 'user' or 'assistant'") String role,
        @NotBlank @Size(max = 1000, message = "must be at most 1000 characters") String content
    ) {}
}
