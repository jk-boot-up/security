package com.jk.explore.security.ssrfspring;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/** The first version: fetch whatever address the customer gives, and follow whatever redirect comes back. */
class NaiveImporter implements Importer {

    static final int MAX_REDIRECTS = 5;
    final Dns dns;
    final Router router;
    final Fetcher fetcher;
    final List<String> trace = new ArrayList<>();

    NaiveImporter(Dns dns, Router router, Fetcher fetcher) {
        this.dns = dns;
        this.router = router;
        this.fetcher = fetcher;
    }

    @Override
    public List<String> trace() {
        return List.copyOf(trace);
    }

    @Override
    public String importImage(String address) throws ImportRefused {
        trace.clear();
        String current = address;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            Target target = Target.parse(current);
            if (hop == 0) {
                check(target); // only the address the customer typed, never where a redirect leads
            }
            Fetcher.Reply reply = fetch(target);
            if (reply.status() / 100 == 3 && reply.location() != null) {
                current = target.redirectTo(reply.location());
                trace.add(reply.status() + " redirect to " + Target.parse(current).host() + ": followed");
                continue;
            }
            trace.add(reply.status() + " " + reply.contentType() + ", " + reply.body().length() + " bytes");
            return "imported: " + reply.body();
        }
        throw new ImportRefused("too many redirects");
    }

    /** The naive importer looks at nothing. Later versions override this. */
    void check(Target target) throws ImportRefused {
    }

    private Fetcher.Reply fetch(Target target) throws ImportRefused {
        try {
            List<InetAddress> found = dns.resolve(target.host());
            InetAddress first = found.get(0);
            trace.add("look up " + target.host() + ": " + found.stream().map(InetAddress::getHostAddress).toList());
            trace.add("GET " + target.path() + " at " + first.getHostAddress() + ", Host: " + target.host());
            return fetcher.get(router.route(first, target.port()), target.host(), target.path());
        } catch (UnknownHostException e) {
            throw new ImportRefused("unknown host " + target.host());
        } catch (Fetcher.NoAnswer e) {
            throw new ImportRefused(e.getMessage());
        }
    }
}
