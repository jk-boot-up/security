package com.jk.explore.security.dnscoredns;

/** The text of the files CoreDNS is started with: a Corefile (its configuration) and a zone file for shop.example. */
final class ShopZone {

    static final String ZONE = "shop.example";
    static final String GOOD_ADDRESS = "203.0.113.10";
    static final String WRONG_ADDRESS = "192.0.2.66";
    static final int GOOD_TTL = 300;
    static final int WRONG_TTL = 86400;
    static final int CACHE_SECONDS = 60;

    private ShopZone() {
    }

    /**
     * One server block for the zone: the file plugin serves it, rewrite maps an old name to the new one, cache keeps answers
     * for up to CACHE_SECONDS, and errors logs problems. Names outside the zone are not served.
     */
    static String corefile() {
        return """
                %s:53 {
                    file /etc/coredns/%s.db
                    rewrite name exact legacy.%s. www.%s.
                    cache %d
                    errors
                }
                """.formatted(ZONE, ZONE, ZONE, ZONE, CACHE_SECONDS);
    }

    /** A zone file with one record for www; the other records never change. */
    static String zoneFile(String wwwAddress, int wwwTtl) {
        return """
                $ORIGIN %s.
                @   3600 IN SOA ns.%s. admin.%s. 1 7200 3600 1209600 3600
                @   3600 IN NS ns.%s.
                ns  3600 IN A 127.0.0.1
                www %d IN A %s
                """.formatted(ZONE, ZONE, ZONE, ZONE, wwwTtl, wwwAddress);
    }
}
