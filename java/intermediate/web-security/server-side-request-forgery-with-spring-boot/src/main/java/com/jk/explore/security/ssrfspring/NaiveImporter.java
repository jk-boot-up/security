package com.jk.explore.security.ssrfspring;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** The first version: fetch whatever address the customer gives, and follow whatever redirect comes back. */
class NaiveImporter implements Importer {

    static final int MAX_REDIRECTS = 5;
    final Dns dns;
    final Router router;
    final Fetcher fetcher;

    NaiveImporter(Dns dns, Router router, Fetcher fetcher) {
        this.dns = dns;
        this.router = router;
        this.fetcher = fetcher;
    }

    @Override
    public String importImage(String address) throws ImportRefused {
        String current = address;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            Target target = Target.parse(current);
            if (hop == 0) {
                check(target); // only the address the customer typed, never where a redirect leads
            }
            Fetcher.Reply reply = fetch(target);
            if (reply.status() / 100 == 3 && reply.location() != null) {
                current = target.redirectTo(reply.location());
                continue;
            }
            return "imported: " + reply.body();
        }
        throw new ImportRefused("too many redirects");
    }

    /** The naive importer looks at nothing. Later versions override this. */
    void check(Target target) throws ImportRefused {
    }

    private Fetcher.Reply fetch(Target target) throws ImportRefused {
        try {
            InetAddress first = dns.resolve(target.host()).get(0);
            return fetcher.get(router.route(first, target.port()), target.host(), target.path());
        } catch (UnknownHostException e) {
            throw new ImportRefused("unknown host " + target.host());
        } catch (Fetcher.NoAnswer e) {
            throw new ImportRefused(e.getMessage());
        }
    }
}
