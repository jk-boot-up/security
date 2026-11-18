package com.jk.explore.security.depconfusion;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A teaching class: what dependency verification compares. Gradle keeps a SHA-256 for each file in gradle/verification-metadata.xml;
 * this class reads it back and computes the SHA-256 of the bytes a repository serves, so the demo can show both values side by side.
 */
final class Checksums {
    private Checksums() {
    }

    static String sha256(byte[] bytes) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The SHA-256 the metadata lists for one file of group:name:version, or null when that version is not listed. */
    static String listed(String metadataXml, String group, String name, String version, String file) {
        Matcher component = Pattern.compile("<component group=\"" + Pattern.quote(group) + "\" name=\"" + Pattern.quote(name)
                + "\" version=\"" + Pattern.quote(version) + "\">(.*?)</component>", Pattern.DOTALL).matcher(metadataXml);
        if (!component.find()) {
            return null;
        }
        Matcher sha = Pattern.compile("<artifact name=\"" + Pattern.quote(file) + "\">\\s*<sha256 value=\"([0-9a-f]{64})\"").matcher(component.group(1));
        return sha.find() ? sha.group(1) : null;
    }
}
