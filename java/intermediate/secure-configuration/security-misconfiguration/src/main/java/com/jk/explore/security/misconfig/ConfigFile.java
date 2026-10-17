package com.jk.explore.security.misconfig;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;
import java.util.Set;

/** The settings file on disk. It holds the admin secret, so who can read it matters as much as what it says. */
final class ConfigFile {

    private ConfigFile() {
    }

    static void write(Path file, Properties settings, String permissions) throws IOException {
        try (Writer out = Files.newBufferedWriter(file)) {
            settings.store(out, null);
        }
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString(permissions));
    }

    static String permissions(Path file) throws IOException {
        return PosixFilePermissions.toString(Files.getPosixFilePermissions(file));
    }

    static boolean readableByOthers(Path file) throws IOException {
        Set<PosixFilePermission> p = Files.getPosixFilePermissions(file);
        return p.contains(PosixFilePermission.GROUP_READ) || p.contains(PosixFilePermission.OTHERS_READ);
    }
}
