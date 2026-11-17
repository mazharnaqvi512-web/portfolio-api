package com.mazhar.portfolio.dto;

import java.util.List;

/** Consistent response envelope every endpoint returns — matches the Angular side's expected shape. */
public record ApiResponse<T>(T data, boolean success, String message, List<String> errors) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, true, null, null);
    }

    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return new ApiResponse<>(null, false, message, errors);
    }
}
