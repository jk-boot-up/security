package com.jk.explore.security.securitylogging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

/**
 * A log file created readable and writable by its owner only, and written only by appending. Appending is a habit of the code, not
 * a guarantee: the operating system's append-only flag needs root, and anyone with root can rewrite the file, which is why logs are
 * also shipped off the host.
 */
final class FileSink implements LogSink {

    private final Path file;

    FileSink(Path file) throws IOException {
        this.file = file;
        Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
    }

    @Override
    public void write(String line) throws IOException {
        Files.writeString(file, line + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    List<String> lines() throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    }

    String permissions() throws IOException {
        return PosixFilePermissions.toString(Files.getPosixFilePermissions(file));
    }

    Path file() {
        return file;
    }
}
