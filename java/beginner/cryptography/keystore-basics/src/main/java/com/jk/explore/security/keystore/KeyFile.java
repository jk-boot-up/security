package com.jk.explore.security.keystore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Keeping the key in a file outside the program. Better than source code, since the key is no longer in the jar, but the file is only as
 * private as its permissions: the owner alone may read it, or anyone on the machine can.
 */
public final class KeyFile {

    private KeyFile() {
    }

    /** Writes the key with the permissions that the operating system gives a new file by default, usually readable by others. */
    public static Path writeWithDefaultPermissions(Path file, byte[] key) throws IOException {
        Files.write(file, key);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));
        return file;
    }

    /** Writes the key so that only the owner can read or write it. */
    public static Path writeOwnerOnly(Path file, byte[] key) throws IOException {
        Files.write(file, key);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        return file;
    }

    public static byte[] read(Path file) throws IOException {
        return Files.readAllBytes(file);
    }

    /** True if anyone other than the owner can read the file. */
    public static boolean readableByOthers(Path file) throws IOException {
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file);
        return permissions.contains(PosixFilePermission.GROUP_READ) || permissions.contains(PosixFilePermission.OTHERS_READ);
    }

    public static String permissions(Path file) throws IOException {
        return PosixFilePermissions.toString(Files.getPosixFilePermissions(file));
    }
}
