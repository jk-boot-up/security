package com.jk.explore.security.certlife;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** The CA database (openssl's index.txt) written directly, to see how big a CRL gets without issuing 100,000 certificates. */
final class Crls {

    private static final DateTimeFormatter DB_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss'Z'").withZone(ZoneOffset.UTC);

    private Crls() {
    }

    /**
     * The database of a CA that revoked this many certificates, of which the given number belong to certificates whose own dates have passed.
     * Serial numbers run 1000, 1001, ...
     */
    static String index(int revoked, int alreadyExpired, Instant now) {
        StringBuilder sb = new StringBuilder();
        String revokedAt = DB_TIME.format(now.minus(1, ChronoUnit.DAYS));
        for (int i = 0; i < revoked; i++) {
            Instant expires = i < alreadyExpired ? now.minus(10, ChronoUnit.DAYS) : now.plus(300, ChronoUnit.DAYS);
            sb.append("R\t").append(DB_TIME.format(expires)).append('\t').append(revokedAt).append("\t")
                    .append("%08X".formatted(0x1000 + i)).append("\tunknown\t/CN=revoked-").append(i).append('\n');
        }
        return sb.toString();
    }
}
