package com.jk.explore.security.apisec;

/** A status and a body; {@code retryAfterSeconds} is set on 429. */
public record ApiResponse(int status, Object body, int retryAfterSeconds) {

    public static ApiResponse ok(Object body) {
        return new ApiResponse(200, body, 0);
    }

    public static ApiResponse error(int status, String message) {
        return new ApiResponse(status, message, 0);
    }
}
