package com.jk.explore.security.certsbc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Calls the openssl command-line tool, a different program written by different people, as an independent check on what Bouncy Castle writes. */
final class OpenSsl {

    record Result(int exit, String output) {
    }

    private OpenSsl() {
    }

    static boolean available() {
        try {
            Result result = run(null, "version");
            return result.exit() == 0 && result.output().startsWith("OpenSSL 3");
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /** Runs openssl in a directory and returns everything it printed, error output included. */
    static Result run(Path dir, String... arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("openssl");
        command.addAll(List.of(arguments));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        if (dir != null) {
            builder.directory(dir.toFile());
        }
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes());
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            return new Result(-1, output);
        }
        return new Result(process.exitValue(), output);
    }
}
