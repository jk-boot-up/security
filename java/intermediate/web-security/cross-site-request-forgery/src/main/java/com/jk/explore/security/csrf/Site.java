package com.jk.explore.security.csrf;

import java.net.URI;
import java.util.Locale;

/** Origins and sites, as a browser compares them. A site is the registrable domain: the last two labels of the host, here. */
final class Site {

    private Site() {
    }

    static String origin(String url) {
        URI uri = URI.create(url);
        return uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT);
    }

    static String host(String url) {
        return URI.create(url).getHost().toLowerCase(Locale.ROOT);
    }

    /** bank.example for reviews.bank.example. Real browsers use the Public Suffix List; two labels are enough for these names. */
    static String registrable(String host) {
        String[] labels = host.split("\\.");
        return labels.length <= 2 ? host : labels[labels.length - 2] + "." + labels[labels.length - 1];
    }

    static boolean sameSite(String originA, String urlB) {
        return URI.create(originA).getScheme().equals(URI.create(urlB).getScheme())
                && registrable(Site.host(originA)).equals(registrable(Site.host(urlB)));
    }

    /** The value a browser puts in Sec-Fetch-Site for a request from a page at initiator (null: the user typed the address). */
    static String fetchSite(String initiator, String url) {
        if (initiator == null) {
            return "none";
        }
        if (initiator.equals(origin(url))) {
            return "same-origin";
        }
        return sameSite(initiator, url) ? "same-site" : "cross-site";
    }
}
