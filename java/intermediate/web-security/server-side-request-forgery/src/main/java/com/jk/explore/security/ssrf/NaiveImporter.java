package com.jk.explore.security.ssrf;

/** The first version: fetch whatever address the customer gives, and follow every redirect. */
public final class NaiveImporter implements Importer {

    private final Dns dns;
    private final Network network;

    public NaiveImporter(Dns dns, Network network) {
        this.dns = dns;
        this.network = network;
    }

    @Override
    public String importImage(String address) {
        Url url = Url.parse(address);
        for (int hops = 0; hops < 10; hops++) {
            Response r = network.get(dns.resolve(url.host()).get(0), url.path());
            if (!r.isRedirect()) {
                return r.body();
            }
            url = url.resolve(r.location());
        }
        throw new ImportRefused("too many redirects");
    }
}
