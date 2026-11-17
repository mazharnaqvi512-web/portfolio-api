package com.mazhar.portfolio.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.mazhar.portfolio.ai.AiChatService;
import com.mazhar.portfolio.ai.ChatTurn;
import com.mazhar.portfolio.dto.ApiResponse;
import com.mazhar.portfolio.dto.ChatRequest;
import com.mazhar.portfolio.dto.ChatResponse;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiChatService chatService;

    public AiController(AiChatService chatService) {
        this.chatService = chatService;
    }

    /** Rate limited per address (see RateLimitConfig) and capped per day (see DailyCap). */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        List<ChatTurn> turns = request.messages().stream()
            .map(message -> new ChatTurn(
                "user".equals(message.role()) ? ChatTurn.Role.USER : ChatTurn.Role.ASSISTANT,
                message.content().strip()))
            .toList();
        return ResponseEntity.ok(ApiResponse.success(new ChatResponse(chatService.chat(turns))));
    }
}
