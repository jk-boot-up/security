package com.jk.explore.security.ssrfspring;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

/**
 * The fixed version. The name is resolved once, every address it resolves to is judged, and the connection goes to the address that was
 * judged, so nothing can change between the check and the use. Every redirect is judged again from the start, and the answer has to be a
 * small image.
 */
class GuardedImporter implements Importer {

    static final Set<Integer> PORTS = Set.of(80, 443);
    static final int MAX_REDIRECTS = 3;
    private final Dns dns;
    private final Router router;
    private final Fetcher fetcher;

    GuardedImporter(Dns dns, Router router, Fetcher fetcher) {
        this.dns = dns;
        this.router = router;
        this.fetcher = fetcher;
    }

    @Override
    public String importImage(String address) throws ImportRefused {
        String current = address;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            Target target = Target.parse(current);
            if (target.userInfo() != null) {
                throw new ImportRefused("user info in the address");
            }
            if (!PORTS.contains(target.port())) {
                throw new ImportRefused("port " + target.port() + " is not allowed");
            }
            InetAddress chosen = resolveAndJudge(target.host());
            Fetcher.Reply reply;
            try {
                reply = fetcher.get(router.route(chosen, target.port()), target.host(), target.path());
            } catch (Fetcher.NoAnswer e) {
                throw new ImportRefused(e.getMessage());
            }
            if (reply.status() / 100 == 3 && reply.location() != null) {
                current = target.redirectTo(reply.location());
                continue;
            }
            if (reply.status() != 200) {
                throw new ImportRefused("the server answered " + reply.status());
            }
            if (reply.tooLarge()) {
                throw new ImportRefused("larger than " + Fetcher.MAX_BYTES + " bytes");
            }
            if (!reply.contentType().startsWith("image/")) {
                throw new ImportRefused("not an image (" + reply.contentType().split(";")[0] + ")");
            }
            return "imported: " + reply.body();
        }
        throw new ImportRefused("too many redirects");
    }

    private InetAddress resolveAndJudge(String host) throws ImportRefused {
        List<InetAddress> addresses;
        try {
            addresses = dns.resolve(host);
        } catch (UnknownHostException e) {
            throw new ImportRefused("unknown host " + host);
        }
        for (InetAddress candidate : addresses) {
            if (!AddressPolicy.isPublic(candidate)) {
                throw new ImportRefused(host + " resolves to a private address " + candidate.getHostAddress());
            }
        }
        return addresses.get(0);
    }
}
