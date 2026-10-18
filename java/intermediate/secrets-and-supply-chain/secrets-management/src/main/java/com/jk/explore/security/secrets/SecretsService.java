package com.jk.explore.security.secrets;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A small secrets service, run in the same process to keep the lab self-contained. An application proves who it is with
 * its own credential, then asks for a database login. Each login is new, belongs to one application, and comes with a
 * lease: it works for a time to live, can be renewed up to a maximum, and is removed from the database when the lease ends.
 * Every request is written to an audit trail.
 */
final class SecretsService {

    record Lease(String id, String app, String user, Secret password, long issuedAt, long expiresAt) {
    }

    static final class RefusedException extends Exception {
        RefusedException(String message) {
            super(message);
        }
    }

    private static final String ALPHABET = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SimClock clock;
    private final Database database;
    private final long ttl;
    private final long maxTtl;
    private final Map<String, Secret> appCredentials = new HashMap<>();
    private final Map<String, Lease> leases = new HashMap<>();
    private final List<String> audit = new ArrayList<>();
    private final SecureRandom random = new SecureRandom();
    private int counter;

    SecretsService(SimClock clock, Database database, long ttl, long maxTtl) {
        this.clock = clock;
        this.database = database;
        this.ttl = ttl;
        this.maxTtl = maxTtl;
    }

    void registerApp(String app, Secret credential) {
        appCredentials.put(app, credential);
    }

    /** A new database login for one application, if its credential is right. */
    Lease leaseDatabaseLogin(String app, Secret credential) throws RefusedException {
        Secret expected = appCredentials.get(app);
        if (expected == null || !expected.matches(credential)) {
            audit.add("t=" + clock.now() + " " + app + " refused: wrong application credential");
            throw new RefusedException("wrong application credential");
        }
        counter++;
        String user = "v-" + app + "-" + counter;
        Secret password = Secret.of(randomPassword(24));
        long now = clock.now();
        Lease lease = new Lease("lease-" + counter, app, user, password, now, now + ttl);
        database.createAccount(user, password, lease.expiresAt());
        leases.put(lease.id(), lease);
        audit.add("t=" + now + " " + app + " leased " + user + " until t=" + lease.expiresAt());
        return lease;
    }

    /** Extends a lease by one time to live, but never past the maximum counted from when it was issued. */
    Lease renew(String leaseId) throws RefusedException {
        Lease lease = live(leaseId);
        long expiresAt = Math.min(clock.now() + ttl, lease.issuedAt() + maxTtl);
        Lease renewed = new Lease(lease.id(), lease.app(), lease.user(), lease.password(), lease.issuedAt(), expiresAt);
        leases.put(leaseId, renewed);
        database.extend(lease.user(), expiresAt);
        audit.add("t=" + clock.now() + " " + lease.app() + " renewed " + lease.user() + " until t=" + expiresAt);
        return renewed;
    }

    void revoke(String leaseId) {
        Lease lease = leases.remove(leaseId);
        if (lease != null) {
            database.dropAccount(lease.user());
            audit.add("t=" + clock.now() + " " + lease.app() + " revoked " + lease.user());
        }
    }

    /** Removes the database logins of every lease that has ended, as the service does on a timer. */
    int expireLeases() {
        List<String> ended = leases.values().stream().filter(l -> clock.now() >= l.expiresAt()).map(Lease::id).sorted().toList();
        for (String id : ended) {
            Lease lease = leases.remove(id);
            database.dropAccount(lease.user());
            audit.add("t=" + clock.now() + " " + lease.app() + " lease ended, " + lease.user() + " removed");
        }
        return ended.size();
    }

    Optional<Lease> lease(String id) {
        return Optional.ofNullable(leases.get(id));
    }

    List<String> audit() {
        return List.copyOf(audit);
    }

    private Lease live(String leaseId) throws RefusedException {
        Lease lease = leases.get(leaseId);
        if (lease == null || clock.now() >= lease.expiresAt()) {
            throw new RefusedException("the lease has ended");
        }
        return lease;
    }

    private char[] randomPassword(int length) {
        char[] out = new char[length];
        for (int i = 0; i < length; i++) {
            out[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
        }
        return out;
    }
}
