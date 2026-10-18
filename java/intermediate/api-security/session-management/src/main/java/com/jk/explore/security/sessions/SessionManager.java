package com.jk.explore.security.sessions;

import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

/**
 * The bank's session manager. Every rule the project teaches lives here: random ids stored only as hashes, a new id at sign-in and at
 * step-up, idle and absolute timeouts, the password again for payments, and sign-out that ends the session on the server.
 */
final class SessionManager {

    /** A password change: the new id for the session that made it, and how many other sessions it ended. */
    record Changed(String id, int ended) {
    }

    private final SimClock clock;
    private final Settings settings;
    private final SessionStore store;
    private final Users users;

    SessionManager(SimClock clock, Settings settings, SessionStore store, Users users) {
        this.clock = clock;
        this.settings = settings;
        this.store = store;
        this.users = users;
    }

    SessionStore store() {
        return store;
    }

    /** A session for a visitor who has not signed in yet. */
    synchronized String start() {
        return create(null, clock.now());
    }

    private String create(String user, long createdAt) {
        String id = SessionIds.newId();
        byte[] hash = SessionIds.hash(id);
        store.put(SessionIds.key(hash), new Session(hash, user, createdAt, clock.now()));
        return id;
    }

    /** The stored session for an id, found by the id's hash and compared in constant time, or null. Timeouts are not checked here. */
    private Session find(String id) {
        if (id == null) {
            return null;
        }
        byte[] hash = SessionIds.hash(id);
        Session session = store.get(SessionIds.key(hash));
        return session != null && MessageDigest.isEqual(session.idHash, hash) ? session : null;
    }

    private void remove(String id) {
        if (id != null) {
            store.remove(SessionIds.key(SessionIds.hash(id)));
        }
    }

    /** Checks an id against the store and both timeouts. A session that has timed out is deleted. */
    private Answer check(String id) {
        Session session = find(id);
        if (session == null) {
            return Answer.UNKNOWN;
        }
        long now = clock.now();
        if (now - session.lastSeenAt >= settings.idleSeconds()) {
            remove(id);
            return Answer.IDLE_EXPIRED;
        }
        if (now - session.createdAt >= settings.absoluteSeconds()) {
            remove(id);
            return Answer.ABSOLUTE_EXPIRED;
        }
        session.lastSeenAt = now;
        return Answer.ALLOWED;
    }

    /** Signs a customer in from the session the browser already has. Returns the id the browser must use from now on. */
    synchronized Optional<String> signIn(String currentId, String user, char[] password) {
        if (!users.check(user, password)) {
            return Optional.empty();
        }
        long now = clock.now();
        String id;
        Session current = check(currentId) == Answer.ALLOWED ? find(currentId) : null;
        if (settings.rotate() || current == null) {
            remove(currentId);
            id = create(user, now);
        } else {
            current.user = user;
            current.authenticatedAt = now;
            id = currentId;
        }
        enforceLimit(user, SessionIds.key(SessionIds.hash(id)));
        return Optional.of(id);
    }

    /** Ends the oldest sessions of a customer who has more than the limit, never the one just made. */
    private void enforceLimit(String user, String keep) {
        List<String> keys = store.keys();
        int count = (int) keys.stream().filter(k -> user.equals(store.get(k).user)).count();
        for (String key : keys) {
            if (count <= settings.maxSessions()) {
                return;
            }
            if (!key.equals(keep) && user.equals(store.get(key).user)) {
                store.remove(key);
                count--;
            }
        }
    }

    /** Reading the account: any signed-in session that has not timed out. */
    synchronized Answer account(String id) {
        Answer answer = check(id);
        if (answer != Answer.ALLOWED) {
            return answer;
        }
        return find(id).signedIn() ? Answer.ALLOWED : Answer.NOT_SIGNED_IN;
    }

    /** Making a payment: also needs the password to have been given recently. */
    synchronized Answer payment(String id) {
        Answer answer = account(id);
        if (answer != Answer.ALLOWED) {
            return answer;
        }
        return clock.now() - find(id).authenticatedAt > settings.reauthSeconds() ? Answer.REAUTH_REQUIRED : Answer.ALLOWED;
    }

    /** Giving the password again before a payment. The session keeps its age, and gets a new id. */
    synchronized Optional<String> stepUp(String id, char[] password) {
        if (account(id) != Answer.ALLOWED) {
            return Optional.empty();
        }
        Session session = find(id);
        if (!users.check(session.user, password)) {
            return Optional.empty();
        }
        if (!settings.rotate()) {
            session.authenticatedAt = clock.now();
            return Optional.of(id);
        }
        remove(id);
        String next = create(session.user, session.createdAt);
        find(next).authenticatedAt = clock.now();
        return Optional.of(next);
    }

    /** Signing out ends the session on the server, so a copy of the cookie is worthless afterwards. */
    synchronized void signOut(String id) {
        remove(id);
    }

    /** Signing out everywhere ends every session of the customer. Returns how many were ended. */
    synchronized int signOutEverywhere(String user) {
        return store.removeIf(e -> user.equals(e.getValue().user));
    }

    /** Changing the password ends every other session of the customer and gives this one a new id. */
    synchronized Optional<Changed> changePassword(String id, char[] oldPassword, char[] newPassword) {
        if (account(id) != Answer.ALLOWED) {
            return Optional.empty();
        }
        Session session = find(id);
        if (!users.check(session.user, oldPassword)) {
            return Optional.empty();
        }
        users.set(session.user, newPassword);
        String keep = SessionIds.key(session.idHash);
        int ended = store.removeIf(e -> session.user.equals(e.getValue().user) && !e.getKey().equals(keep));
        remove(id);
        String next = create(session.user, session.createdAt);
        find(next).authenticatedAt = clock.now();
        return Optional.of(new Changed(next, ended));
    }
}
