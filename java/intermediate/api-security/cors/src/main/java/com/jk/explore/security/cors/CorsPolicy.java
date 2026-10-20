package com.jk.explore.security.cors;

import java.util.Optional;
import java.util.Set;

/** Decides which {@code Origin} header values the bank API will name in {@code Access-Control-Allow-Origin}. */
public interface CorsPolicy {

    /** The value for Access-Control-Allow-Origin, or empty to send no CORS headers. */
    Optional<String> allow(String origin);

    /** No CORS headers at all: the browser's default, same-origin only. */
    CorsPolicy NONE = origin -> Optional.empty();

    /** Echo whatever origin asks. A common "it fixes the error" change. */
    CorsPolicy REFLECT_ANY = origin -> origin == null ? Optional.empty() : Optional.of(origin);

    /** An exact list of sites, which also lets the opaque "null" origin through (sandboxed frames, local files, redirects). */
    static CorsPolicy exactAndNull(Set<String> origins) {
        return origin -> origin != null && (origins.contains(origin) || origin.equals("null")) ? Optional.of(origin) : Optional.empty();
    }

    /** Matches the end of the text: "https://evilbank.example" ends with "bank.example" too. */
    static CorsPolicy suffix(String suffix) {
        return origin -> origin != null && origin.endsWith(suffix) ? Optional.of(origin) : Optional.empty();
    }

    /** The fixed policy: exact, case-sensitive comparison with a list; anything else, including "null", gets no headers. */
    static CorsPolicy exact(Set<String> origins) {
        return origin -> origin != null && origins.contains(origin) ? Optional.of(origin) : Optional.empty();
    }
}
