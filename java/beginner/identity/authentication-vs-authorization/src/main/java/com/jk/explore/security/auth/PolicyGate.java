package com.jk.explore.security.auth;

import java.util.Optional;

/**
 * Authentication, then authorization. First find out who is asking (401 if nobody), then decide whether
 * that person may do this to this order (403 if not). The role is either the one copied into the session at
 * login, which is fast but can go stale, or looked up in the directory on every request, which is current.
 */
public class PolicyGate implements Gate {

    public enum RoleSource {
        SESSION,
        DIRECTORY
    }

    private final Authenticator authenticator;
    private final UserDirectory directory;
    private final RoleSource roleSource;

    public PolicyGate(Authenticator authenticator, UserDirectory directory, RoleSource roleSource) {
        this.authenticator = authenticator;
        this.directory = directory;
        this.roleSource = roleSource;
    }

    @Override
    public Response check(String token, Action action, Order order) {
        Optional<Session> session = authenticator.authenticate(token);
        if (session.isEmpty()) {
            return new Response(Status.UNAUTHENTICATED, "no live session");
        }
        String user = session.get().username();
        Role role = roleSource == RoleSource.SESSION ? session.get().roleAtLogin() : directory.find(user).role();
        if (allowed(role, user, action, order)) {
            return new Response(Status.OK, user + " may " + action.name().toLowerCase());
        }
        return new Response(Status.FORBIDDEN, user + " may not " + action.name().toLowerCase());
    }

    /** The rules: owners may view and cancel their own orders; support may view any and refund; admins may do all. */
    static boolean allowed(Role role, String user, Action action, Order order) {
        boolean owner = order.owner().equals(user);
        return switch (role) {
            case ADMIN -> true;
            case SUPPORT -> action == Action.VIEW || action == Action.REFUND;
            case CUSTOMER -> owner && (action == Action.VIEW || action == Action.CANCEL);
        };
    }
}
