package com.jk.explore.security.localca;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A list of every certificate the store runs, with the ones that need attention flagged. Most certificate outages are expiries nobody saw coming. */
final class Inventory {

    enum Status { FINE, RENEW_NOW, EXPIRED }

    record Entry(String name, Status status, String left) {
    }

    private Inventory() {
    }

    static Status status(X509Certificate certificate, Instant now) {
        if (!now.isBefore(certificate.getNotAfter().toInstant())) {
            return Status.EXPIRED;
        }
        return now.isBefore(ShortLived.renewAt(certificate)) ? Status.FINE : Status.RENEW_NOW;
    }

    static List<Entry> report(List<X509Certificate> certificates, Instant now) {
        List<Entry> entries = new ArrayList<>();
        for (X509Certificate certificate : certificates) {
            Duration left = Duration.between(now, certificate.getNotAfter().toInstant());
            String name = certificate.getSubjectX500Principal().getName().replace("CN=", "");
            String text = left.isNegative() || left.isZero() ? "0 hours" : CertificateAuthority.describe(Duration.ofHours(left.toHours()));
            entries.add(new Entry(name, status(certificate, now), text));
        }
        return entries;
    }
}
