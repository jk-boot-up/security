package com.jk.explore.security.hostscan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** The docker command, as a program. */
final class Docker {

    private Docker() {
    }

    static String run(String... command) throws IOException, InterruptedException {
        List<String> full = new ArrayList<>();
        full.add("docker");
        full.addAll(List.of(command));
        Process p = new ProcessBuilder(full).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(300, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException(String.join(" ", full) + " failed: " + out);
        }
        return out;
    }

    /** Runs a script as root inside a running container and returns what it printed. */
    static String exec(String container, String script) throws IOException, InterruptedException {
        return run("exec", "-u", "0", container, "sh", "-c", script);
    }
}
