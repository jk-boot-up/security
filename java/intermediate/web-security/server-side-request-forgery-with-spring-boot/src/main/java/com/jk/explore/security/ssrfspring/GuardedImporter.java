package com.jk.explore.security.ssrfspring;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * The fixed version. The name is resolved once, every address it resolves to is judged, and the connection goes to the address that was
 * judged, so nothing can change between the check and the use. Every redirect is judged again from the start, and the answer has to be a
 * small image. Each decision is written to a trace, as a security log would record it.
 */
class GuardedImporter implements Importer {

    static final List<Integer> PORTS = List.of(80, 443);
    static final int MAX_REDIRECTS = 3;
    private final Dns dns;
    private final Router router;
    private final Fetcher fetcher;
    private final List<String> trace = new ArrayList<>();

    GuardedImporter(Dns dns, Router router, Fetcher fetcher) {
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
            if (target.userInfo() != null) {
                throw refused("user info in the address");
            }
            if (!PORTS.contains(target.port())) {
                throw refused("port " + target.port() + " is not allowed");
            }
            trace.add("port " + target.port() + ", no user info: allowed");
            InetAddress chosen = resolveAndJudge(target.host());
            trace.add("GET " + target.path() + " at " + chosen.getHostAddress() + ", Host: " + target.host());
            Fetcher.Reply reply;
            try {
                reply = fetcher.get(router.route(chosen, target.port()), target.host(), target.path());
            } catch (Fetcher.NoAnswer e) {
                throw refused(e.getMessage());
            }
            if (reply.status() / 100 == 3 && reply.location() != null) {
                current = target.redirectTo(reply.location());
                trace.add(reply.status() + " redirect to " + Target.parse(current).host() + ": check it again");
                continue;
            }
            if (reply.status() != 200) {
                throw refused("the server answered " + reply.status());
            }
            if (reply.tooLarge()) {
                throw refused("larger than " + Fetcher.MAX_BYTES + " bytes");
            }
            if (!reply.contentType().startsWith("image/")) {
                throw refused("not an image (" + reply.contentType().split(";")[0] + ")");
            }
            trace.add(reply.status() + " " + reply.contentType() + ", " + reply.body().length() + " bytes: imported");
            return "imported: " + reply.body();
        }
        throw refused("too many redirects");
    }

    private InetAddress resolveAndJudge(String host) throws ImportRefused {
        List<InetAddress> addresses;
        try {
            addresses = dns.resolve(host);
        } catch (UnknownHostException e) {
            throw refused("unknown host " + host);
        }
        trace.add("look up " + host + " once: " + addresses.stream().map(InetAddress::getHostAddress).toList());
        for (InetAddress candidate : addresses) {
            if (!AddressPolicy.isPublic(candidate)) {
                String address = candidate.getHostAddress();
                throw refused(host.equals(address) ? address + " is not public" : host + " is " + address + ", not public");
            }
        }
        InetAddress chosen = addresses.get(0);
        trace.add(chosen.getHostAddress() + " is public: connect to it");
        return chosen; // the connection goes to this very address: the name is never looked up again
    }

    private ImportRefused refused(String reason) {
        trace.add("refused: " + reason);
        return new ImportRefused(reason);
    }
}
