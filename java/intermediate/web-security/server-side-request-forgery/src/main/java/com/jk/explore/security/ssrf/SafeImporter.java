package com.jk.explore.security.ssrf;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;

/**
 * The fixed version. It resolves the name once, checks <em>every</em> address it got, connects to the address it checked (so a second DNS answer
 * cannot change the target), re-checks every redirect, and accepts only a small image.
 */
public final class SafeImporter implements Importer {

    static final int MAX_REDIRECTS = 3;
    static final int MAX_BYTES = 2000;
    private static final Set<Integer> PORTS = Set.of(80, 443);

    private final Dns dns;
    private final Network network;

    public SafeImporter(Dns dns, Network network) {
        this.dns = dns;
        this.network = network;
    }

    @Override
    public String importImage(String address) {
        Url url = Url.parse(address);
        for (int hops = 0; hops <= MAX_REDIRECTS; hops++) {
            Response r = network.get(checkedAddress(url), url.path());
            if (r.isRedirect()) {
                url = url.resolve(r.location());
                continue;
            }
            if (r.status() != 200) {
                throw new ImportRefused("the server answered " + r.status());
            }
            if (!r.contentType().startsWith("image/")) {
                throw new ImportRefused("not an image: " + r.contentType());
            }
            if (r.body().length() > MAX_BYTES) {
                throw new ImportRefused("image too large");
            }
            return r.body();
        }
        throw new ImportRefused("too many redirects");
    }

    private InetAddress checkedAddress(Url url) {
        if (!url.scheme().equals("http") && !url.scheme().equals("https")) {
            throw new ImportRefused("scheme " + url.scheme() + " is not allowed");
        }
        if (url.userInfo() != null) {
            throw new ImportRefused("user info in the address");
        }
        if (!PORTS.contains(url.port())) {
            throw new ImportRefused("port " + url.port() + " is not allowed");
        }
        List<InetAddress> addresses = dns.resolve(url.host());
        for (InetAddress a : addresses) {
            if (!AddressPolicy.isPublic(a)) {
                throw new ImportRefused(url.host() + " resolves to a private address " + a.getHostAddress());
            }
        }
        return addresses.get(0); // the connection goes to this very address: it is never looked up again
    }
}
