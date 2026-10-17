package com.jk.explore.security.auth;

/** No checks at all: nobody is asked who they are, and everything is allowed. */
public class NoGate implements Gate {
    @Override
    public Response check(String token, Action action, Order order) {
        return new Response(Status.OK, "nobody asked who");
    }
}
