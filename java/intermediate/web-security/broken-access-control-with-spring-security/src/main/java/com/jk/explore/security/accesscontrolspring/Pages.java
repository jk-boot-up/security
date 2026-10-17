package com.jk.explore.security.accesscontrolspring;

import org.springframework.security.core.Authentication;

/** What the store's page shows each person: the admin links are hidden from everyone else, which is presentation, not protection. */
final class Pages {

    static String links(Authentication who) {
        boolean admin = who.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return admin ? "orders, account, users, refunds, reports" : "orders, account (the admin links are hidden)";
    }

    private Pages() {
    }
}
