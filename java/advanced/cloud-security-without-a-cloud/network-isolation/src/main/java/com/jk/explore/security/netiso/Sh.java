package com.jk.explore.security.netiso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** The docker command, as a program. */
final class Sh {

    /** What a command printed and its exit code. */
    record Result(int exit, String output) {

        boolean ok() {
            return exit == 0;
        }
    }

    private Sh() {
    }

    static Result run(String... command) throws IOException, InterruptedException {
        List<String> full = new ArrayList<>(List.of("docker"));
        full.addAll(List.of(command));
        Process p = new ProcessBuilder(full).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(120, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IllegalStateException("did not finish: " + full);
        }
        return new Result(p.exitValue(), out);
    }

    static String must(String... command) throws IOException, InterruptedException {
        Result r = run(command);
        if (!r.ok()) {
            throw new IllegalStateException("docker " + String.join(" ", command) + " failed: " + r.output());
        }
        return r.output();
    }
}
