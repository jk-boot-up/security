package com.jk.explore.security.tlsaudit;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A teaching class: redoes by hand three checks that testssl.sh only reports, so the demo can show how each one is made. */
final class WorkedOut {

    static final long DAY = 86_400;

    private WorkedOut() {
    }

    /** The HSTS max-age and testssl's minimum, read from a finding like "730 days (=63072000 seconds) > 15552000 seconds". */
    record Hsts(long seconds, long minimum) {

        long days() {
            return seconds / DAY;
        }

        long minimumDays() {
            return minimum / DAY;
        }

        boolean ok() {
            return seconds >= minimum;
        }
    }

    static Hsts hsts(String finding) {
        Matcher m = Pattern.compile("=(\\d+) seconds\\) > (\\d+) seconds").matcher(finding);
        if (!m.find()) {
            throw new IllegalArgumentException("no max-age in: " + finding);
        }
        return new Hsts(Long.parseLong(m.group(1)), Long.parseLong(m.group(2)));
    }

    /** Is the name asked for one of the certificate's names? A wildcard covers exactly one label. */
    static boolean listed(String asked, String subjectAltNames) {
        return Arrays.stream(subjectAltNames.split(" ")).anyMatch(name -> name.equalsIgnoreCase(asked)
            || name.startsWith("*.") && asked.indexOf('.') > 0
                        && asked.substring(asked.indexOf('.') + 1).equalsIgnoreCase(name.substring(2)));
    }

    /** The certificate's lifetime in hours, from testssl's "2026-11-13 05:22" dates. */
    static long lifetimeHours(String notBefore, String notAfter) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return Duration.between(LocalDateTime.parse(notBefore, f), LocalDateTime.parse(notAfter, f)).toHours();
    }
}
