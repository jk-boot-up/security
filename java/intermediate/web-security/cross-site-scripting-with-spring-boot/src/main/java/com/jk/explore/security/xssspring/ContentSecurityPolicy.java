package com.jk.explore.security.xssspring;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * A strict Content-Security-Policy with a fresh nonce for every response, and an evaluator that answers what a browser would:
 * may this inline script run? Only scripts carrying this response's nonce may.
 */
final class ContentSecurityPolicy {

    private static final SecureRandom RANDOM = new SecureRandom();

    private ContentSecurityPolicy() {
    }

    /** 128 random bits, Base64: unguessable, and different for every response. */
    static String newNonce() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    static String strict(String nonce) {
        return "default-src 'self'; script-src 'nonce-" + nonce + "' 'strict-dynamic'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'";
    }

    /** The source list that governs scripts: script-src, or default-src when the policy has no script-src. */
    static List<String> scriptSources(String policy) {
        List<String> fallback = null;
        for (String part : policy.split(";")) {
            String[] words = part.strip().split("\\s+");
            if (words.length == 0 || words[0].isEmpty()) {
                continue;
            }
            String directive = words[0].toLowerCase(Locale.ROOT);
            List<String> sources = List.of(words).subList(1, words.length);
            if (directive.equals("script-src")) {
                return sources;
            }
            if (directive.equals("default-src")) {
                fallback = sources;
            }
        }
        return fallback;
    }

    /**
     * Would a browser run an inline script that carries this nonce (null for none)? With no policy, yes. A matching nonce, yes.
     * 'unsafe-inline' allows any inline script, unless a nonce or hash is also listed, in which case browsers ignore it.
     */
    static boolean allowsInlineScript(String policy, String scriptNonce) {
        if (policy == null) {
            return true;
        }
        List<String> sources = scriptSources(policy);
        if (sources == null) {
            return true;
        }
        boolean hasNonceOrHash = sources.stream().anyMatch(s -> s.startsWith("'nonce-") || s.startsWith("'sha"));
        if (scriptNonce != null && sources.contains("'nonce-" + scriptNonce + "'")) {
            return true;
        }
        return sources.contains("'unsafe-inline'") && !hasNonceOrHash;
    }
}
