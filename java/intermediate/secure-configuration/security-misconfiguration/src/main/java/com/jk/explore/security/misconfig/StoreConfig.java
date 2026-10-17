package com.jk.explore.security.misconfig;

import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * The store's settings, read from a properties file. Each setting is a switch that a developer finds useful and an attacker
 * finds just as useful: a debug page, a directory listing, detailed errors, a version banner, the TRACE method.
 */
record StoreConfig(String profile, String adminPassword, boolean debugEndpoint, boolean directoryListing, boolean detailedErrors,
        String serverBanner, boolean traceAllowed, Set<String> testSettings) {

    static final String ADMIN_PASSWORD = "admin.password";
    static final String DEBUG_ENDPOINT = "debug.endpoint";
    static final String STATIC_LISTING = "static.listing";
    static final String ERRORS_DETAILED = "errors.detailed";
    static final String SERVER_BANNER = "server.banner";
    static final String HTTP_TRACE = "http.trace";
    static final String PROFILE = "profile";

    /** The settings every profile must state, so that nothing falls back to a default nobody chose. */
    static final Set<String> KNOWN = Set.of(PROFILE, ADMIN_PASSWORD, DEBUG_ENDPOINT, STATIC_LISTING, ERRORS_DETAILED, SERVER_BANNER, HTTP_TRACE);

    static StoreConfig from(Properties p) {
        Set<String> tests = new TreeSet<>();
        for (String key : p.stringPropertyNames()) {
            if (key.startsWith("test.")) {
                tests.add(key);
            }
        }
        return new StoreConfig(
                p.getProperty(PROFILE, "dev").strip().toLowerCase(Locale.ROOT),
                p.getProperty(ADMIN_PASSWORD, ""),
                flag(p, DEBUG_ENDPOINT),
                flag(p, STATIC_LISTING),
                flag(p, ERRORS_DETAILED),
                p.getProperty(SERVER_BANNER, "").strip(),
                flag(p, HTTP_TRACE),
                Set.copyOf(tests));
    }

    private static boolean flag(Properties p, String key) {
        return "true".equals(p.getProperty(key, "false").strip().toLowerCase(Locale.ROOT));
    }
}
