package com.jk.explore.security.leastprivilege;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/** Operating system permissions: the oldest least-privilege tool, and one the JDK can set directly. */
final class OwnerOnly {

    private OwnerOnly() {
    }

    static String lockFile(Path file) throws IOException {
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        return PosixFilePermissions.toString(Files.getPosixFilePermissions(file));
    }

    static String lockDirectory(Path directory) throws IOException {
        Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
        return PosixFilePermissions.toString(Files.getPosixFilePermissions(directory));
    }

    static String openFile(Path file) throws IOException {
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));
        return PosixFilePermissions.toString(Files.getPosixFilePermissions(file));
    }

    static boolean readableByOthers(Path file) throws IOException {
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file);
        return permissions.contains(PosixFilePermission.OTHERS_READ) || permissions.contains(PosixFilePermission.GROUP_READ);
    }
}
