package com.jk.explore.security.cia;

/** Thrown when a user asks for something their role does not allow. */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
