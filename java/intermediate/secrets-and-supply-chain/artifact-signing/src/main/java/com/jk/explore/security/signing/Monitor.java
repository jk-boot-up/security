package com.jk.explore.security.signing;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The store's own watcher of the public log. It knows which releases the store really made, and reports every entry signed with one of
 * the store's keys that is not one of them: a stolen or misused key cannot sign in secret if every deployment requires a log entry.
 */
final class Monitor {

    private final KeyRing storeKeys;
    private final Set<String> knownDigests;

    Monitor(KeyRing storeKeys, Set<String> knownDigests) {
        this.storeKeys = storeKeys;
        this.knownDigests = knownDigests;
    }

    List<String> unexpected(TransparencyLog log) {
        List<String> findings = new ArrayList<>();
        for (int i = 0; i < log.size(); i++) {
            String text = new String(log.entry(i), StandardCharsets.UTF_8);
            String keyId = text.substring(0, text.indexOf('\n'));
            if (!storeKeys.isStoreKey(keyId)) {
                continue;
            }
            int start = text.indexOf("sha256=") + 7;
            String digest = text.substring(start, start + 64);
            int versionStart = text.indexOf("version=") + 8;
            String version = text.substring(versionStart, text.indexOf('\n', versionStart));
            if (!knownDigests.contains(digest)) {
                findings.add("entry " + i + ": version " + version + " signed with " + keyId + ", digest " + digest.substring(0, 16)
                        + " is not a release the store made");
            }
        }
        return findings;
    }
}
