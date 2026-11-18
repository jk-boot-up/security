package com.jk.explore.security.provenance;

import java.util.ArrayList;
import java.util.List;

/**
 * A build pipeline described by the facts SLSA build levels depend on. Level one needs provenance; level two needs it signed by a
 * hosted build platform, so the people whose code is built cannot forge it; level three needs a hardened platform where one build cannot
 * reach the signing key or influence another build.
 */
record Pipeline(String name, boolean writesProvenance, boolean hostedPlatformSigns, boolean buildsIsolatedFromSigningKey) {

    int level() {
        if (!writesProvenance) {
            return 0;
        }
        if (!hostedPlatformSigns) {
            return 1;
        }
        return buildsIsolatedFromSigningKey ? 3 : 2;
    }

    /** What stops this pipeline from reaching the next level (a developer could forge the provenance, or a malicious build step could sign
     * anything), or nothing at level three. */
    List<String> missing() {
        List<String> missing = new ArrayList<>();
        if (!writesProvenance) {
            missing.add("no provenance");
        } else if (!hostedPlatformSigns) {
            missing.add("the developer holds the key");
        } else if (!buildsIsolatedFromSigningKey) {
            missing.add("build steps can reach the key");
        }
        return missing;
    }
}
