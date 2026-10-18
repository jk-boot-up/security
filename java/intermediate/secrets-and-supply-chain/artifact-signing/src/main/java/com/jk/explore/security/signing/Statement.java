package com.jk.explore.security.signing;

import java.nio.charset.StandardCharsets;

/**
 * What a release signature vouches for: the artifact's name, version and channel, the SHA-256 of its exact bytes, and the day the
 * signer says it signed. The text form is canonical, one field per line in a fixed order, so the same statement always has the same bytes.
 */
record Statement(String name, String version, String channel, String sha256, int signedDay) {

    static final String HEADER = "release-statement/v1";

    static Statement of(Artifact artifact, String name, String version, String channel, int signedDay) {
        return new Statement(name, version, channel, Digests.hex(artifact.sha256()), signedDay);
    }

    byte[] bytes() {
        return (HEADER + "\nname=" + name + "\nversion=" + version + "\nchannel=" + channel + "\nsha256=" + sha256 + "\nsigned-day=" + signedDay + "\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    /** Reads only the exact canonical form: any other shape is refused. */
    static Statement parse(byte[] bytes) {
        String[] lines = new String(bytes, StandardCharsets.UTF_8).split("\n", -1);
        if (lines.length != 7 || !lines[0].equals(HEADER) || !lines[6].isEmpty()) {
            throw new IllegalArgumentException("not a release statement");
        }
        String name = field(lines[1], "name");
        String version = field(lines[2], "version");
        String channel = field(lines[3], "channel");
        String sha256 = field(lines[4], "sha256");
        if (!sha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("not a SHA-256 digest");
        }
        int day;
        try {
            day = Integer.parseInt(field(lines[5], "signed-day"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a day");
        }
        Statement statement = new Statement(name, version, channel, sha256, day);
        if (!java.util.Arrays.equals(statement.bytes(), bytes)) {
            throw new IllegalArgumentException("not in canonical form");
        }
        return statement;
    }

    private static String field(String line, String key) {
        if (!line.startsWith(key + "=")) {
            throw new IllegalArgumentException("expected " + key);
        }
        return line.substring(key.length() + 1);
    }
}
