package com.jk.explore.security.depconf;

/** The build refused to resolve a dependency. */
public class ResolutionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ResolutionException(String message) {
        super(message);
    }
}
