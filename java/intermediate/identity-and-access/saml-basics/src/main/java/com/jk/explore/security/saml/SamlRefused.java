package com.jk.explore.security.saml;

/** The sign-in response was refused. The reason is for the security log; the customer only sees "sign-in failed". */
public class SamlRefused extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SamlRefused(String reason) {
        super(reason);
    }
}
