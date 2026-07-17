package com.agrodairy.common.dto;

import java.util.List;

public record ApiError(String code, String message, List<FieldError> details) {

    public record FieldError(String field, String message) {}

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }

    public static ApiError of(String code, String message, List<FieldError> details) {
        return new ApiError(code, message, details);
    }
}
