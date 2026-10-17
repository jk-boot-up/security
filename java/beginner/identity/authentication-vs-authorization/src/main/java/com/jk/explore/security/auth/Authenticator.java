package com.jk.explore.security.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Authentication: finding out who is asking. A correct password gives a session token; later requests
 * show the token instead of the password. Tokens expire after a fixed number of ticks, and can be ended
 * early. Tokens here are numbered so the demo is repeatable; real tokens must be long and random.
 */
public class Authenticator {

    private final Credentials credentials;
    private final UserDirectory directory;
    private final Ticker ticker;
    private final int lifetimeTicks;
    private final Map<String, Session> sessions = new HashMap<>();
    private int issued;

    public Authenticator(Credentials credentials, UserDirectory directory, Ticker ticker, int lifetimeTicks) {
        this.credentials = credentials;
        this.directory = directory;
        this.ticker = ticker;
        this.lifetimeTicks = lifetimeTicks;
    }

    public Optional<Session> login(String username, String password) {
        if (!credentials.verify(username, password)) {
            return Optional.empty();
        }
        Account account = directory.find(username);
        Session session = new Session("tok-" + (++issued), username, account.role(), ticker.now());
        sessions.put(session.token(), session);
        return Optional.of(session);
    }

    /** The live session for a token, or nothing if there is none, it has expired, or it was ended. */
    public Optional<Session> authenticate(String token) {
        Session session = token == null ? null : sessions.get(token);
        if (session == null || ticker.now() - session.issuedAt() >= lifetimeTicks) {
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void logout(String token) {
        sessions.remove(token);
    }
}
