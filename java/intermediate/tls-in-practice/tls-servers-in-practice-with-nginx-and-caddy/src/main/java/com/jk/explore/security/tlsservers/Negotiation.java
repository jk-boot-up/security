package com.jk.explore.security.tlsservers;

import java.util.List;
import java.util.Optional;

/** How client and server pick a protocol version, redone by hand: the highest version both of them speak, or none. */
final class Negotiation {

    /** Newest first. */
    static final List<String> VERSIONS = List.of("TLSv1.3", "TLSv1.2");

    private Negotiation() {
    }

    static Optional<String> agree(List<String> server, List<String> client) {
        for (String version : VERSIONS) {
            if (server.contains(version) && client.contains(version)) {
                return Optional.of(version);
            }
        }
        return Optional.empty();
    }
}
