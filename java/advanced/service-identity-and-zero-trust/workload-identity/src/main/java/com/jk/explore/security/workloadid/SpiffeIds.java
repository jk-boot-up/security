package com.jk.explore.security.workloadid;

/** A teaching class: a SPIFFE ID taken apart by hand, the way the java-spiffe library reads it (a test checks they agree). */
final class SpiffeIds {

    /** The two parts that matter: who vouches for the name (the trust domain) and the workload's name inside it (the path). */
    record Parts(String trustDomain, String path) {
    }

    static final String SCHEME = "spiffe://";

    private SpiffeIds() {
    }

    static Parts parts(String id) {
        if (!id.startsWith(SCHEME)) {
            throw new IllegalArgumentException("not a SPIFFE ID: " + id);
        }
        String rest = id.substring(SCHEME.length());
        int slash = rest.indexOf('/');
        if (slash < 0) {
            return new Parts(rest, "");
        }
        return new Parts(rest.substring(0, slash), rest.substring(slash));
    }
}
