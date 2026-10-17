package com.jk.explore.security.signaturesbc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Calls a real OpenSSL 3 command-line tool, a different program written by different people, to check what this code wrote.
 * The system openssl on macOS is LibreSSL, so a Homebrew OpenSSL 3 is looked for too.
 */
final class OpenSsl {

    record Result(int exit, String output) {
    }

    private static final List<String> CANDIDATES = List.of("openssl", "/opt/homebrew/opt/openssl@3/bin/openssl", "/usr/local/opt/openssl@3/bin/openssl");

    private OpenSsl() {
    }

    /** The command of an OpenSSL 3, or null when there is none. */
    static String find() {
        for (String candidate : CANDIDATES) {
            try {
                Result result = run(List.of(candidate, "version"));
                if (result.exit() == 0 && result.output().startsWith("OpenSSL 3")) {
                    return candidate;
                }
            } catch (IOException | InterruptedException e) {
                // try the next candidate
            }
        }
        return null;
    }

    static Result run(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try {
            byte[] out = process.getInputStream().readAllBytes();
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                return new Result(-1, "timed out");
            }
            return new Result(process.exitValue(), new String(out).strip());
        } finally {
            process.destroyForcibly();
        }
    }

    /** Runs openssl with the given arguments, in a directory that is deleted afterwards. */
    static Result openssl(Path dir, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(find());
        command.addAll(List.of(args));
        Result result = run(command);
        return result;
    }

    static void deleteAll(Path dir) throws IOException {
        try (var files = Files.list(dir)) {
            for (Path p : files.toList()) {
                Files.deleteIfExists(p);
            }
        }
        Files.deleteIfExists(dir);
    }
}
