package com.jk.explore.security.ssrf;

import java.util.List;

/**
 * The second version, a blocklist of host <em>names</em>: it checks the text of the address, not the address the connection really goes to.
 * It stops the three examples in the bug report and nothing else.
 */
public final class BlocklistImporter implements Importer {

    static final List<String> BLOCKED = List.of("localhost", "127.0.0.1", "169.254.169.254", "0.0.0.0");
    static final List<String> BLOCKED_STARTS = List.of("10.", "192.168.");

    private final Dns dns;
    private final Network network;

    public BlocklistImporter(Dns dns, Network network) {
        this.dns = dns;
        this.network = network;
    }

    @Override
    public String importImage(String address) {
        Url url = Url.parse(address);
        if (BLOCKED.contains(url.host()) || BLOCKED_STARTS.stream().anyMatch(url.host()::startsWith)) {
            throw new ImportRefused("blocked host name " + url.host());
        }
        for (int hops = 0; hops < 10; hops++) {
            Response r = network.get(dns.resolve(url.host()).get(0), url.path());
            if (!r.isRedirect()) {
                return r.body();
            }
            url = url.resolve(r.location()); // a redirect is followed without a second look
        }
        throw new ImportRefused("too many redirects");
    }
}
