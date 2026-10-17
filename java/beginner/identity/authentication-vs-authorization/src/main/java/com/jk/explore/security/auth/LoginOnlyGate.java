package com.jk.explore.security.auth;

/** Authentication only: anyone with a live session is let in, whatever they ask for. This is the common mistake. */
public class LoginOnlyGate implements Gate {

    private final Authenticator authenticator;

    public LoginOnlyGate(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public Response check(String token, Action action, Order order) {
        return authenticator.authenticate(token)
                .map(s -> new Response(Status.OK, "logged in as " + s.username()))
                .orElseGet(() -> new Response(Status.UNAUTHENTICATED, "no live session"));
    }
}
