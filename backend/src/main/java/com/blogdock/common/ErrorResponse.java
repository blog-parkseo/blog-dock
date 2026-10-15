package com.blogdock.common;

import java.util.List;

/** 모든 오류를 같은 모양으로 돌려준다 (COM-02). */
public record ErrorResponse(String code, String message, List<FieldError> fields) {

    public record FieldError(String field, String reason) {
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of());
    }
}
