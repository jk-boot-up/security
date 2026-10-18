package com.jk.explore.security.oidc;

import java.util.List;

/** An ID token that passed every check. The user is identified by issuer and subject together; the email is only a contact detail. */
record IdToken(String issuer, String subject, String email, boolean emailVerified, String sid, long authTime, String acr, List<String> amr) {

    String identity() {
        return issuer + " | " + subject;
    }
}
