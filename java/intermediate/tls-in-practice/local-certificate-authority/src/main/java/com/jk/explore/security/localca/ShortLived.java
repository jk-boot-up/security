package com.jk.explore.security.localca;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * A service that keeps a 24-hour certificate and renews it on its own, with a new key each time, once two thirds of its life has passed.
 * If the renewal job stops, the certificate simply runs out: no revocation list is needed to end it.
 */
final class ShortLived {

    static final Duration LIFETIME = Duration.ofHours(24);

    private final CertificateAuthority authority;
    private final String requester;
    private final String name;
    private X509Certificate current;
    private int renewals;
    private boolean jobRunning = true;

    ShortLived(CertificateAuthority authority, String requester, String name) {
        this.authority = authority;
        this.requester = requester;
        this.name = name;
    }

    /** When a certificate should be renewed: two thirds of the way from notBefore to notAfter. */
    static Instant renewAt(X509Certificate certificate) {
        Instant from = certificate.getNotBefore().toInstant();
        Instant to = certificate.getNotAfter().toInstant();
        return from.plus(Duration.between(from, to).multipliedBy(2).dividedBy(3));
    }

    /** One run of the renewal job. Returns true if it renewed. */
    boolean tick(Instant now) throws RefusedException {
        if (!jobRunning) {
            return false;
        }
        if (current == null || !now.isBefore(renewAt(current))) {
            boolean first = current == null;
            current = authority.issue(Csr.create(Keys.newPair(), List.of(name)), requester, Profile.SHORT_LIVED, LIFETIME, now);
            if (!first) {
                renewals++;
            }
            return true;
        }
        return false;
    }

    void stopJob() {
        jobRunning = false;
    }

    X509Certificate current() {
        return current;
    }

    int renewals() {
        return renewals;
    }
}
