package com.jk.explore.security.depconf;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * A published package: a name, a version, its contents, and an optional install script that the build runs when it installs the package
 * (npm {@code postinstall}, Python {@code setup.py}). The digest is the SHA-256 of everything in it.
 */
public record Artifact(String name, Version version, String content, String installScript) {

    public static Artifact of(String name, String version, String content) {
        return new Artifact(name, Version.parse(version), content, null);
    }

    public Artifact withInstallScript(String script) {
        return new Artifact(name, version, content, script);
    }

    public String digest() {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            d.update((name + "@" + version + "\n" + content + "\n" + installScript).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
