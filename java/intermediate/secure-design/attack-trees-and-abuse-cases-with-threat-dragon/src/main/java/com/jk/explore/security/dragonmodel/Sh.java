package com.jk.explore.security.dragonmodel;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs a program and returns what it printed. */
final class Sh {

    private Sh() {
    }

    static String run(List<String> command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(300, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + out);
        }
        return out;
    }
}
