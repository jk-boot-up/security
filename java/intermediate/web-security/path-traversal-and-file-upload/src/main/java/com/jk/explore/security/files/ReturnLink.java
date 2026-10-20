package com.jk.explore.security.files;

import java.net.URI;
import java.util.Set;

/** The "return to" address after sign-in or upload: where the customer is sent next. */
public final class ReturnLink {

    private static final String HOME = "/account";
    private static final Set<String> OWN_HOSTS = Set.of("shop.example");

    private ReturnLink() {
    }

    /** The first version: go wherever the link says. */
    public static String naive(String returnTo) {
        return returnTo;
    }

    /**
     * The fixed version: only a path on this site. Browsers read {@code //host} and {@code /\host} as other sites, and a scheme names one, so all three are refused;
     * anything unusable becomes the home page instead of an error.
     */
    public static String safe(String returnTo) {
        if (returnTo == null || returnTo.isEmpty() || returnTo.length() > 200) {
            return HOME;
        }
        if (!returnTo.startsWith("/") || returnTo.startsWith("//") || returnTo.contains("\\") || returnTo.chars().anyMatch(c -> c < 0x20)) {
            return HOME;
        }
        try {
            URI uri = new URI(returnTo);
            if (uri.getScheme() != null || uri.getAuthority() != null) {
                return HOME;
            }
            return uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        } catch (java.net.URISyntaxException e) {
            return HOME;
        }
    }

    /** For an absolute address that really must be allowed: the host is compared exactly, never by prefix or suffix. */
    public static boolean isOwnHost(String address) {
        try {
            URI uri = new URI(address);
            return "https".equals(uri.getScheme()) && uri.getUserInfo() == null && OWN_HOSTS.contains(uri.getHost());
        } catch (java.net.URISyntaxException e) {
            return false;
        }
    }
}
