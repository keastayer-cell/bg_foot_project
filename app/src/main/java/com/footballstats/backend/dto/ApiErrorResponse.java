package com.footballstats.backend.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record ApiErrorResponse(
    OffsetDateTime timestamp,
    int status,
    String error,
    String path,
    Map<String, String> fieldErrors
) {
    public static ApiErrorResponse of(int status, String error, String path) {
        return new ApiErrorResponse(OffsetDateTime.now(), status, error, path, Map.of());
    }
}
