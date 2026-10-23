package com.jk.explore.security.certlife;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** An inventory of certificates and the expiry monitor that reads it: buckets by time left, and a day-by-day model of what the monitor saves. */
final class Fleet {

    /** One certificate of the inventory: whose it is and when it stops working. */
    record Entry(String name, Instant notAfter) {
    }

    /** Where a certificate is on its way to expiry. */
    enum Bucket {
        EXPIRED("expired"), UNDER_7_DAYS("under 7 days"), UNDER_30_DAYS("under 30 days"), OK("30 days or more");

        final String label;

        Bucket(String label) {
            this.label = label;
        }
    }

    /** What the day-by-day model found. */
    record Model(int expiringInHorizon, int outages, int prevented) {
    }

    private Fleet() {
    }

    static Bucket bucket(Instant notAfter, Instant now) {
        Duration left = Duration.between(now, notAfter);
        if (left.isNegative() || left.isZero()) {
            return Bucket.EXPIRED;
        } else if (left.compareTo(Duration.ofDays(7)) < 0) {
            return Bucket.UNDER_7_DAYS;
        } else if (left.compareTo(Duration.ofDays(30)) < 0) {
            return Bucket.UNDER_30_DAYS;
        }
        return Bucket.OK;
    }

    static Map<Bucket, Integer> count(List<Entry> fleet, Instant now) {
        Map<Bucket, Integer> counts = new EnumMap<>(Bucket.class);
        for (Bucket b : Bucket.values()) {
            counts.put(b, 0);
        }
        for (Entry e : fleet) {
            counts.merge(bucket(e.notAfter(), now), 1, Integer::sum);
        }
        return counts;
    }

    /** Reads every .crt file of a directory (the inventory). */
    static List<Entry> scan(Path dir) throws IOException, GeneralSecurityException {
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(f -> f.getFileName().toString().endsWith(".crt")).sorted().toList();
        }
        List<Entry> entries = new ArrayList<>();
        for (Path f : files) {
            entries.add(new Entry(f.getFileName().toString().replace(".crt", ""), Pki.readCertificates(f).getFirst().getNotAfter().toInstant()));
        }
        return entries;
    }

    /**
     * Walks the calendar one day at a time. The monitor scans every morning and alerts on the first scan that finds a certificate with less
     * than alertDays left; the owner then needs ownerDays to renew it. A certificate that expires before that is an outage. Certificates
     * that had already expired at the first scan are not counted here (the first scan finds them).
     */
    static Model model(List<Entry> fleet, Instant start, int horizonDays, int alertDays, int ownerDays, boolean monitored) {
        int expiring = 0;
        int outages = 0;
        for (Entry e : fleet) {
            if (!e.notAfter().isAfter(start) || e.notAfter().isAfter(start.plus(horizonDays, ChronoUnit.DAYS))) {
                continue;
            }
            expiring++;
            if (!monitored) {
                outages++;
                continue;
            }
            Instant renewedAt = null;
            for (int day = 0; day <= horizonDays; day++) {
                Instant scan = start.plus(day, ChronoUnit.DAYS);
                if (Duration.between(scan, e.notAfter()).compareTo(Duration.ofDays(alertDays)) < 0) {
                    renewedAt = scan.plus(ownerDays, ChronoUnit.DAYS);
                    break;
                }
            }
            if (renewedAt == null || renewedAt.isAfter(e.notAfter())) {
                outages++;
            }
        }
        return new Model(expiring, outages, expiring - outages);
    }
}
