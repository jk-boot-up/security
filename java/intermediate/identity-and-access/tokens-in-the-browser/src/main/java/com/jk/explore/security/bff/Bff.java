package com.jk.explore.security.bff;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A backend for the frontend. The OAuth client lives here, on the server, as a confidential client: it signs the user in, keeps the
 * access and refresh tokens in its own session store, and gives the browser only a session cookie. Every API call from the page goes
 * through it, and it adds the access token on the way.
 */
final class Bff {

    static final String ORIGIN = "https://app.bank.example";
    static final String COOKIE = "__Host-bff";
    static final String REQUEST_HEADER = "X-Bank-Request";
    static final String ACCOUNTS = ORIGIN + "/bff/accounts";
    static final String TRANSFERS = ORIGIN + "/bff/transfers";

    private record Session(String subject, AuthServer.Tokens tokens) {
    }

    private final AuthServer authServer;
    private final BankApi api;
    private final SimClock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<String, Long> accessExpiry = new HashMap<>();

    Bff(AuthServer authServer, BankApi api, SimClock clock) {
        this.authServer = authServer;
        this.api = api;
        this.clock = clock;
    }

    /** Signs the user in (the code flow runs between this server and the authorization server) and returns the Set-Cookie value. */
    String signIn(String subject) {
        byte[] id = new byte[32];
        random.nextBytes(id);
        String sessionId = Base64Url.encode(id);
        AuthServer.Tokens tokens = authServer.issue(subject, "accounts.read payments.write", null);
        String key = Bytes.sha256Base64Url(sessionId);
        sessions.put(key, new Session(subject, tokens));
        accessExpiry.put(key, clock.now() + AuthServer.ACCESS_SECONDS);
        return COOKIE + "=" + sessionId + "; Path=/; Secure; HttpOnly; SameSite=Strict";
    }

    /** How many tokens this server holds for its sessions. None of them is ever written into a response to the browser. */
    int tokensHeld() {
        return sessions.size() * 2;
    }

    /** The BFF answers cross-origin preflights for no other origin, so another site's script can never add the request header. */
    boolean allowsCrossOrigin(String origin) {
        return false;
    }

    Response handle(Request request) {
        String sessionId = request.cookies().get(COOKIE);
        String key = sessionId == null ? null : Bytes.sha256Base64Url(sessionId);
        Session session = key == null ? null : sessions.get(key);
        if (session == null) {
            return Response.of(401, "not signed in");
        }
        if (request.changesState() && !"1".equals(request.header(REQUEST_HEADER))) {
            return Response.of(403, "a change needs the bank's own request header");
        }
        String target;
        if (request.method().equals("GET") && request.url().equals(ACCOUNTS)) {
            target = BankApi.ACCOUNTS;
        } else if (request.method().equals("POST") && request.url().equals(TRANSFERS)) {
            target = BankApi.TRANSFERS;
        } else {
            return Response.of(404, "no such route");
        }
        Optional<String> access = accessToken(key, session);
        if (access.isEmpty()) {
            sessions.remove(key);
            return Response.of(401, "the sign-in has ended");
        }
        return api.handle(Request.of(request.method(), target).withHeader("Authorization", "Bearer " + access.get()));
    }

    private Optional<String> accessToken(String key, Session session) {
        if (clock.now() < accessExpiry.get(key)) {
            return Optional.of(session.tokens().accessToken());
        }
        Optional<AuthServer.Tokens> fresh = authServer.refresh(session.tokens().refreshToken(), null);
        fresh.ifPresent(tokens -> {
            sessions.put(key, new Session(session.subject(), tokens));
            accessExpiry.put(key, clock.now() + AuthServer.ACCESS_SECONDS);
        });
        return fresh.map(AuthServer.Tokens::accessToken);
    }
}
