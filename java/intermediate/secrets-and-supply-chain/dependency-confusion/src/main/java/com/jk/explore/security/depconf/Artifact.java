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

    /** The three lines the digest covers: name@version, the contents, the install script (empty when there is none). */
    public String digestInput() {
        String script = installScript == null ? "" : installScript;
        return name + "@" + version + "\n" + content + "\n" + script;
    }

    public String digest() {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digestInput().getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(d.digest(bytes));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
