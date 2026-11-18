package com.mazhar.portfolio.exception;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.mazhar.portfolio.dto.ApiResponse;

/** Converts Bean Validation failures into the same response envelope every other endpoint uses, instead of Spring's default error shape. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(RequestTooLargeException.class)
    public ResponseEntity<ApiResponse<Void>> handleTooLarge(RequestTooLargeException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiResponse.error("The request is too large.", List.of()));
    }

    @ExceptionHandler(InvalidChatRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidChat(InvalidChatRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage(), List.of()));
    }

    @ExceptionHandler(AiDailyLimitException.class)
    public ResponseEntity<ApiResponse<Void>> handleDailyLimit(AiDailyLimitException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ApiResponse.error(
            "The assistant has reached its daily limit. Please try again tomorrow, or use the Contact page.", List.of()));
    }

    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnavailable(AiUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(ex.getMessage(), List.of()));
    }

    /** A fixed message on purpose: the cause (an SDK error, maybe with request details) is logged by the provider, never echoed. */
    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ApiResponse<Void>> handleProviderFailure(AiProviderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.error(
            "The assistant is temporarily unavailable. Please try again shortly.", List.of()));
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
