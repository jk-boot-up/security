package com.jk.explore.security.uploadspring;

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
        return reason(returnTo) == null ? returnTo : HOME;
    }

    /** Why a link is not a path on this site, or {@code null} when it is one. */
    public static String reason(String returnTo) {
        if (returnTo == null || returnTo.isEmpty()) {
            return "empty";
        }
        if (returnTo.length() > 200) {
            return "too long";
        }
        if (!returnTo.startsWith("/")) {
            return "does not start with /";
        }
        if (returnTo.startsWith("//")) {
            return "starts with //: another host";
        }
        if (returnTo.contains("\\")) {
            return "has a backslash: browsers read /\\ as //";
        }
        if (returnTo.chars().anyMatch(c -> c < 0x20)) {
            return "has a control character";
        }
        try {
            URI uri = new URI(returnTo);
            return uri.getScheme() != null || uri.getAuthority() != null ? "names a scheme or a host" : null;
        } catch (java.net.URISyntaxException e) {
            return "not a valid link";
        }
    }

    /** For an absolute address that really must be allowed: the host is compared exactly, never by prefix or suffix. */
    public static boolean isOwnHost(String address) {
        try {
            URI uri = new URI(address);
            return "https".equals(uri.getScheme())
                    && uri.getUserInfo() == null
                    && OWN_HOSTS.contains(uri.getHost());
        } catch (java.net.URISyntaxException e) {
            return false;
        }
    }
}
