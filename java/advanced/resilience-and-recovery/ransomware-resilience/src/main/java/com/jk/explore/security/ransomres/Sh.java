package com.jk.explore.security.ransomres;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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

    static Result run(Path dir, Map<String, String> env, List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(true);
        if (dir != null) {
            pb.directory(dir.toFile());
        }
        pb.environment().putAll(env);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes());
        if (!p.waitFor(600, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IllegalStateException("did not finish: " + command);
        }
        return new Result(p.exitValue(), out);
    }

    static Result run(List<String> command) throws IOException, InterruptedException {
        return run(null, Map.of(), command);
    }

    static Result run(String... command) throws IOException, InterruptedException {
        return run(List.of(command));
    }

    /** Like run, but a failure is an error. */
    static String must(List<String> command) throws IOException, InterruptedException {
        return must(null, Map.of(), command);
    }

    static String must(Path dir, Map<String, String> env, List<String> command) throws IOException, InterruptedException {
        Result r = run(dir, env, command);
        if (!r.ok()) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + r.output());
        }
        return r.output();
    }
}
