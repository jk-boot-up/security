package com.jk.explore.security.sessions;

/** One session as the server stores it. The id itself is never stored, only its hash. */
final class Session {

    final byte[] idHash;
    final long createdAt;
    String user;
    long lastSeenAt;
    long authenticatedAt;

    Session(byte[] idHash, String user, long createdAt, long now) {
        this.idHash = idHash;
        this.user = user;
        this.createdAt = createdAt;
        this.lastSeenAt = now;
        this.authenticatedAt = user == null ? -1 : now;
    }

    boolean signedIn() {
        return user != null;
    }
}
