package com.jk.explore.security.ssrf;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The fixed version. It resolves the name once, checks <em>every</em> address it got, connects to the address it checked (so a second DNS answer
 * cannot change the target), re-checks every redirect, and accepts only a small image. Each decision is written to a trace, as a security
 * log would record it.
 */
public final class SafeImporter implements Importer {

    static final int MAX_REDIRECTS = 3;
    static final int MAX_BYTES = 2000;
    private static final Set<Integer> PORTS = Set.of(80, 443);

    private final Dns dns;
    private final Network network;
    private final List<String> trace = new ArrayList<>();

    public SafeImporter(Dns dns, Network network) {
        this.dns = dns;
        this.network = network;
    }

    /** The decisions of the last import, one line each. */
    public List<String> trace() {
        return List.copyOf(trace);
    }

    @Override
    public String importImage(String address) {
        trace.clear();
        Url url = Url.parse(address);
        for (int hops = 0; hops <= MAX_REDIRECTS; hops++) {
            Response r = network.get(checkedAddress(url), url.path());
            if (r.isRedirect()) {
                url = url.resolve(r.location());
                trace.add(r.status() + " redirect to " + url.host() + ": check it again");
                continue;
            }
            if (r.status() != 200) {
                throw refused("the server answered " + r.status());
            }
            if (!r.contentType().startsWith("image/")) {
                throw refused("not an image: " + r.contentType());
            }
            if (r.body().length() > MAX_BYTES) {
                throw refused("image too large");
            }
            trace.add(r.status() + " " + r.contentType() + ", " + r.body().length() + " bytes: imported");
            return r.body();
        }
        throw refused("too many redirects");
    }

    private InetAddress checkedAddress(Url url) {
        if (!url.scheme().equals("http") && !url.scheme().equals("https")) {
            throw refused("scheme " + url.scheme() + " is not allowed");
        }
        if (url.userInfo() != null) {
            throw refused("user info in the address");
        }
        if (!PORTS.contains(url.port())) {
            throw refused("port " + url.port() + " is not allowed");
        }
        trace.add("scheme " + url.scheme() + ", port " + url.port() + ", no user info: allowed");
        List<InetAddress> addresses = dns.resolve(url.host());
        trace.add("look up " + url.host() + " once: " + addresses.stream().map(InetAddress::getHostAddress).toList());
        for (InetAddress a : addresses) {
            String kind = AddressPolicy.kind(a);
            if (!kind.equals("public")) {
                String address = a.getHostAddress();
                throw refused(url.host().equals(address) ? address + " is " + kind : url.host() + " is " + address + ", " + kind);
            }
        }
        InetAddress checked = addresses.get(0);
        trace.add(checked.getHostAddress() + " is public: connect to it");
        return checked; // the connection goes to this very address: it is never looked up again
    }

    private ImportRefused refused(String reason) {
        trace.add("refused: " + reason);
        return new ImportRefused(reason);
    }
}
