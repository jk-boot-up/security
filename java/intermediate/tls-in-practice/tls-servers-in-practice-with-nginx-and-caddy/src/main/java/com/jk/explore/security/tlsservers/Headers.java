package com.jk.explore.security.tlsservers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Two header rules redone by hand: which add_header lines nginx applies to a location, and what an HSTS max-age means. */
final class Headers {

    static final long SECONDS_A_DAY = 86_400;

    private Headers() {
    }

    /** nginx inherits the server's add_header lines only when the location has none of its own. */
    static List<String> effective(List<String> server, List<String> location) {
        return location.isEmpty() ? server : location;
    }

    /** The max-age of a Strict-Transport-Security value, in seconds. */
    static long maxAge(String hsts) {
        Matcher m = Pattern.compile("max-age=(\\d+)").matcher(hsts);
        if (!m.find()) {
            throw new IllegalArgumentException("no max-age in " + hsts);
        }
        return Long.parseLong(m.group(1));
    }

    static boolean includesSubDomains(String hsts) {
        return hsts.toLowerCase().contains("includesubdomains");
    }
}
