package com.jk.explore.security.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * The folders of the store's server, made in a temporary directory so the demo and the tests touch nothing else:
 * <pre>
 * invoices/          what customers may download
 * invoices-private/  a sibling whose name starts the same way
 * secrets/           the database settings
 * config/            the application's settings
 * webroot/           files the web server hands out as they are
 * uploads-private/   where the fixed version keeps uploads, outside the web root
 * </pre>
 */
public record Layout(Path root) implements AutoCloseable {

    public static Layout create() throws IOException {
        Path root = Files.createTempDirectory("files-demo");
        Layout l = new Layout(root);
        for (Path p : new Path[] {l.invoices(), l.invoicesPrivate(), l.secrets(), l.config(), l.webroot(), l.uploadsPrivate(), l.webroot().resolve("uploads")}) {
            Files.createDirectories(p);
        }
        Files.writeString(l.invoices().resolve("invoice-1001.txt"), "Invoice 1001: Ana, 42.50");
        Files.writeString(l.invoices().resolve("invoice-1002.txt"), "Invoice 1002: Ben, 17.00");
        Files.writeString(l.invoicesPrivate().resolve("payroll.txt"), "payroll: demo salaries");
        Files.writeString(l.secrets().resolve("db.txt"), "DB_PASSWORD=demo-not-real");
        Files.writeString(l.config().resolve("app.properties"), "admin=ana");
        return l;
    }

    public Path invoices() {
        return root.resolve("invoices");
    }

    public Path invoicesPrivate() {
        return root.resolve("invoices-private");
    }

    public Path secrets() {
        return root.resolve("secrets");
    }

    public Path config() {
        return root.resolve("config");
    }

    public Path webroot() {
        return root.resolve("webroot");
    }

    public Path uploadsPrivate() {
        return root.resolve("uploads-private");
    }

    @Override
    public void close() throws IOException {
        try (Stream<Path> all = Files.walk(root)) {
            for (Path p : (Iterable<Path>) all.sorted(Comparator.reverseOrder())::iterator) {
                Files.deleteIfExists(p);
            }
        }
    }
}
