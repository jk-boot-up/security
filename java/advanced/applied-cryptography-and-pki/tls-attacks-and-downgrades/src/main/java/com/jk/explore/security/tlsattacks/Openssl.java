package com.jk.explore.security.tlsattacks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds the OpenSSL command line program of the pinned release series (not LibreSSL: it cannot run the weak settings of this lab).
 * The series is pinned because the golden output depends on it: its default TLS 1.3 group, its TLS 1.3 suites, and that it has no RC4 and no zlib.
 */
final class Openssl {

    /** The release series the golden output was made with. */
    static final String SERIES = "3.6";

    private final String binary;

    private Openssl(String binary) {
        this.binary = binary;
    }

    /** The first program of the pinned series: $OPENSSL, Homebrew openssl@3 and openssl@4 (Apple silicon and Intel), then the one on the PATH. */
    static Openssl find() throws IOException, InterruptedException {
        List<String> candidates = new ArrayList<>();
        String env = System.getenv("OPENSSL");
        if (env != null && !env.isBlank()) {
            candidates.add(env);
        }
        for (String p : List.of("/opt/homebrew/opt/openssl@3/bin/openssl", "/usr/local/opt/openssl@3/bin/openssl",
                "/opt/homebrew/opt/openssl@4/bin/openssl", "/usr/local/opt/openssl@4/bin/openssl", "/usr/bin/openssl", "openssl")) {
            candidates.add(p);
        }
        for (String c : candidates) {
            if (c.startsWith("/") && !Files.isExecutable(Path.of(c))) {
                continue;
            }
            try {
                if (isPinned(Sh.run(c, "version").output())) {
                    return new Openssl(c);
                }
            } catch (IOException notThere) {
                // try the next one
            }
        }
        throw new IllegalStateException("OpenSSL " + SERIES + ".x is needed (brew install openssl@3) or set OPENSSL to its path");
    }

    static boolean isPinned(String versionLine) {
        return versionLine.startsWith("OpenSSL " + SERIES + ".");
    }

    String path() {
        return binary;
    }

    List<String> command(String... args) {
        List<String> c = new ArrayList<>();
        c.add(binary);
        c.addAll(List.of(args));
        return c;
    }

    Sh.Result run(String input, String... args) throws IOException, InterruptedException {
        return Sh.run(input, command(args));
    }
}
