package com.jk.explore.security.misconfig;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compares a production settings file with the hardened baseline and with development. A setting that still has its development
 * value, a development-only setting, or a setting the baseline has never seen are all drift.
 */
final class ProfileDrift {

    /** Settings whose production value must differ from the development one. */
    static final List<String> MUST_DIFFER = List.of(StoreConfig.ADMIN_PASSWORD, StoreConfig.DEBUG_ENDPOINT, StoreConfig.STATIC_LISTING,
            StoreConfig.ERRORS_DETAILED, StoreConfig.SERVER_BANNER, StoreConfig.HTTP_TRACE);

    record Report(Set<String> stillDevelopment, Set<String> developmentOnly, Set<String> unreviewed) {
        boolean clean() {
            return stillDevelopment.isEmpty() && developmentOnly.isEmpty() && unreviewed.isEmpty();
        }
    }

    private ProfileDrift() {
    }

    static Report compare(Properties development, Properties baseline, Properties production) {
        Set<String> still = new TreeSet<>();
        for (String key : MUST_DIFFER) {
            String prod = production.getProperty(key);
            if (prod != null && prod.equals(development.getProperty(key))) {
                still.add(key);
            }
        }
        Set<String> devOnly = new TreeSet<>();
        Set<String> unreviewed = new TreeSet<>();
        for (String key : production.stringPropertyNames()) {
            if (key.startsWith("test.")) {
                devOnly.add(key);
            } else if (!baseline.containsKey(key)) {
                unreviewed.add(key);
            }
        }
        return new Report(still, devOnly, unreviewed);
    }
}
