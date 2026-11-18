package com.jk.explore.security.ssrfspring;

import java.util.Locale;
import java.util.List;

/** The second version: refuse a short list of names. The list is read as text, and the network layer reads the address another way. */
class BlocklistImporter extends NaiveImporter {

    static final List<String> BLOCKED = List.of("169.254.169.254", "localhost", "127.0.0.1", "0.0.0.0");

    BlocklistImporter(Dns dns, Router router, Fetcher fetcher) {
        super(dns, router, fetcher);
    }

    @Override
    void check(Target target) throws ImportRefused {
        if (BLOCKED.contains(target.host().toLowerCase(Locale.ROOT))) {
            throw new ImportRefused("blocked host name " + target.host());
        }
    }
}
