package com.jk.explore.security.containerhard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs a program and returns what it printed, whether it worked or not. */
final class Sh {

    /** What a program printed (both streams together) and its exit code. */
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

    static Result run(String... command) throws IOException, InterruptedException {
        return run(List.of(command));
    }

    /** Like run, but a failure is an error. */
    static String must(List<String> command) throws IOException, InterruptedException {
        Result r = run(command);
        if (!r.ok()) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + r.output());
        }
        return r.output();
    }

    static List<String> docker(String... args) {
        List<String> out = new ArrayList<>();
        out.add("docker");
        out.addAll(List.of(args));
        return out;
    }
}
