package com.jk.explore.security.oauth;

import java.util.Locale;

/** The bank's accounts API, the resource server. It trusts nothing in the request except what the authorization server says about the token. */
final class BankApi {

    static final String READ = "accounts.read";
    static final String PAY = "payments.write";

    /** An HTTP-style answer: a status code and a short body. */
    record Answer(int status, String body) {
    }

    private final AuthorizationServer server;

    BankApi(AuthorizationServer server) {
        this.server = server;
    }

    Answer call(String method, String path, String authorization) {
        String token = bearer(authorization);
        AuthorizationServer.Introspection info = server.introspect(token);
        if (!info.active()) {
            return new Answer(401, "invalid_token");
        }
        String needed = switch (method.toUpperCase(Locale.ROOT) + " " + path) {
            case "GET /accounts" -> READ;
            case "POST /payments" -> PAY;
            default -> null;
        };
        if (needed == null) {
            return new Answer(404, "not_found");
        }
        if (!info.scope().contains(needed)) {
            return new Answer(403, "insufficient_scope");
        }
        return READ.equals(needed) ? new Answer(200, info.user() + ": 2 accounts") : new Answer(200, "payment accepted");
    }

    private static String bearer(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
