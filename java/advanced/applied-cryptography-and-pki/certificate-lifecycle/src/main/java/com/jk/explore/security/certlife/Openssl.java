package com.jk.explore.security.certlife;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Finds an OpenSSL 3 or 4 command line program (not LibreSSL: its ca and ocsp commands differ). */
final class Openssl {

    private final String binary;

    private Openssl(String binary) {
        this.binary = binary;
    }

    /** The first usable program: $OPENSSL, Homebrew openssl@3 and openssl@4 (Apple silicon and Intel), then the one on the PATH. */
    static Openssl find() throws IOException, InterruptedException {
        List<String> candidates = new ArrayList<>();
        String env = System.getenv("OPENSSL");
        if (env != null && !env.isBlank()) {
            candidates.add(env);
        }
        candidates.addAll(List.of("/opt/homebrew/opt/openssl@3/bin/openssl", "/usr/local/opt/openssl@3/bin/openssl",
                "/opt/homebrew/opt/openssl@4/bin/openssl", "/usr/local/opt/openssl@4/bin/openssl", "openssl"));
        for (String c : candidates) {
            if (c.startsWith("/") && !Files.isExecutable(Path.of(c))) {
                continue;
            }
            try {
                if (isModern(Sh.run(c, "version").output())) {
                    return new Openssl(c);
                }
            } catch (IOException notThere) {
                // try the next one
            }
        }
        throw new IllegalStateException("OpenSSL 3 or 4 is needed (brew install openssl@3) or set OPENSSL to its path");
    }

    static boolean isModern(String versionLine) {
        return versionLine.startsWith("OpenSSL 3.") || versionLine.startsWith("OpenSSL 4.");
    }

    List<String> command(String... args) {
        List<String> c = new ArrayList<>();
        c.add(binary);
        c.addAll(List.of(args));
        return c;
    }

    Sh.Result run(String... args) throws IOException, InterruptedException {
        return Sh.run(null, java.util.Map.of(), "", command(args));
    }

    /** Runs openssl and fails with its output when it does not work. */
    String must(String... args) throws IOException, InterruptedException {
        return Sh.must(command(args));
    }
}
