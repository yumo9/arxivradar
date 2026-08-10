package com.arxivradar.common;

import java.time.Instant;
import java.util.Map;

public final class ApiError {

    private final int status;
    private final String error;
    private final String message;
    private final Instant timestamp;

    private ApiError(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public static Map<String, Object> of(int status, String error, String message) {
        return Map.of(
                "status", status,
                "error", error,
                "message", message,
                "timestamp", Instant.now().toString()
        );
    }

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }
}
