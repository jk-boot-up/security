package com.jk.explore.security.containerhard;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A seccomp profile that denies the system calls Docker's own default profile denies (mount, unshare, ptrace, kernel modules, keys, bpf, clock changes...).
 * Docker Desktop's daemon may start containers with no profile at all (docker info says "Profile: unconfined"), so the demo passes this one by name
 * and every run has the same filter.
 */
final class Seccomp {

    private Seccomp() {
    }

    /** How many system calls the profile denies: the quoted names in its list. */
    static int denied() throws IOException {
        try (InputStream in = Seccomp.class.getResourceAsStream("/seccomp-default-like.json")) {
            String json = new String(in.readAllBytes());
            String names = json.substring(json.indexOf("\"names\""), json.indexOf(']', json.indexOf("\"names\"")));
            return (int) names.chars().filter(c -> c == '"').count() / 2 - 1;
        }
    }

    static Path profile() throws IOException {
        Path file = Files.createTempFile("seccomp-default-like", ".json");
        try (InputStream in = Seccomp.class.getResourceAsStream("/seccomp-default-like.json")) {
            Files.write(file, in.readAllBytes());
        }
        return file;
    }
}
