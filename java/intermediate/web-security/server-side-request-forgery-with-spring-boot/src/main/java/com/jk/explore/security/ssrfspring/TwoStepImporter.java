package com.jk.explore.security.ssrfspring;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The mistake that looks like a fix: resolve the name and judge it, then hand the same name to the client, which resolves it again.
 * An attacker's name server answers a public address the first time and a private one the second.
 */
class TwoStepImporter extends NaiveImporter {

    TwoStepImporter(Dns dns, Router router, Fetcher fetcher) {
        super(dns, router, fetcher);
    }

    @Override
    void check(Target target) throws ImportRefused {
        try {
            for (InetAddress candidate : dns.resolve(target.host())) {
                if (!AddressPolicy.isPublic(candidate)) {
                    throw new ImportRefused(target.host() + " resolves to a private address " + candidate.getHostAddress());
                }
            }
        } catch (UnknownHostException e) {
            throw new ImportRefused("unknown host " + target.host());
        }
    }
}
