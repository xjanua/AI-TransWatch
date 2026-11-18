package me.xjanua.aitranswatch.dto;

import java.time.OffsetDateTime;

public record RestErrorResponse(boolean success, RestError error, String path, OffsetDateTime timestamp) {
    public static RestErrorResponse of(String code, String message, String path) {
        return new RestErrorResponse(false, new RestError(code, message), path, OffsetDateTime.now());
    }
}
