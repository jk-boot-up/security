package com.jk.explore.security.securitychaos;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs a program and returns what it printed, whether it worked or not. */
final class Sh {

    record Result(int exit, String output) {

        boolean ok() {
            return exit == 0;
        }
    }

    private Sh() {
    }

    static Result run(List<String> command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(300, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IllegalStateException("did not finish: " + command);
        }
        return new Result(p.exitValue(), out);
    }

    static String must(String... command) throws IOException, InterruptedException {
        Result r = run(List.of(command));
        if (!r.ok()) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + r.output());
        }
        return r.output();
    }
}
