package com.jk.explore.security.auth;

/**
 * The answer to a request. 401 means "I do not know who you are" (authentication failed);
 * 403 means "I know who you are, and you may not do this" (authorization failed).
 */
public enum Status {
    OK(200),
    UNAUTHENTICATED(401),
    FORBIDDEN(403);

    private final int code;

    Status(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    @Override
    public String toString() {
        return code + " " + name();
    }
}
