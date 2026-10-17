package com.jk.explore.security.cia;

/** Thrown when a stored order no longer matches the seal made when it was saved. */
public class TamperedOrderException extends RuntimeException {
    public TamperedOrderException(String message) {
        super(message);
    }
}
