package com.jk.explore.security.certlife;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
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

    /** Runs a command with the given text on its standard input, then closes the input. */
    static Result run(Path dir, Map<String, String> env, String input, List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(true);
        if (dir != null) {
            pb.directory(dir.toFile());
        }
        pb.environment().putAll(env);
        Process p = pb.start();
        try (var stdin = p.getOutputStream()) {
            stdin.write(input.getBytes(StandardCharsets.UTF_8));
        } catch (IOException closedEarly) {
            // the program did not want its input; its output tells why
        }
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!p.waitFor(900, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IllegalStateException("did not finish: " + command);
        }
        return new Result(p.exitValue(), out);
    }

    static Result run(String input, List<String> command) throws IOException, InterruptedException {
        return run(null, Map.of(), input, command);
    }

    static Result run(String... command) throws IOException, InterruptedException {
        return run("", List.of(command));
    }

    /** Like run, but a failure is an error. */
    static String must(List<String> command) throws IOException, InterruptedException {
        return must(null, Map.of(), command);
    }

    static String must(Path dir, Map<String, String> env, List<String> command) throws IOException, InterruptedException {
        Result r = run(dir, env, "", command);
        if (!r.ok()) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + r.output());
        }
        return r.output();
    }

    /** Starts a program that keeps running (a server); its output is thrown away. */
    static Process start(List<String> command) throws IOException {
        return new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(Redirect.DISCARD).start();
    }
}
