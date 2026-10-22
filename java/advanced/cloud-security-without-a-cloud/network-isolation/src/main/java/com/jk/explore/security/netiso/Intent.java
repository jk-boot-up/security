package com.jk.explore.security.netiso;

import java.util.Set;
import java.util.TreeSet;

/**
 * What the design says may talk: pairs of services. It is the security-group table of the shop written as code, and everything else is meant to be unreachable.
 * Networks join both directions, so a pair is unordered and is written with the names in alphabetical order, such as app-web.
 */
public record Intent(Set<String> pairs) {

    public static String pair(String a, String b) {
        return a.compareTo(b) < 0 ? a + "-" + b : b + "-" + a;
    }

    /** The shop: the front talks to the application; the application to the database and the cache; the admin panel to the application and the database; the build server to the admin panel. */
    public static Intent shop() {
        return new Intent(new TreeSet<>(Set.of(pair("web", "app"), pair("app", "db"), pair("app", "cache"), pair("admin", "app"), pair("admin", "db"), pair("ci", "admin"))));
    }

    /** Pairs that are reachable and not intended. */
    public Set<String> extra(Set<String> measured) {
        Set<String> out = new TreeSet<>(measured);
        out.removeAll(pairs);
        return out;
    }

    /** Pairs that are intended and not reachable. */
    public Set<String> missing(Set<String> measured) {
        Set<String> out = new TreeSet<>(pairs);
        out.removeAll(measured);
        return out;
    }
}
