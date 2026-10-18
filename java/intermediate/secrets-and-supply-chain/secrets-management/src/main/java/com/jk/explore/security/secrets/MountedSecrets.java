package com.jk.explore.security.secrets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Secrets injected at run time as files in a mounted folder (as container platforms do with /run/secrets), read once at
 * startup. A file that others can read is refused, and the bytes read are wiped once they are turned into a Secret.
 */
final class MountedSecrets {

    static final class UnsafeFileException extends Exception {
        UnsafeFileException(String message) {
            super(message);
        }
    }

    private static final Set<PosixFilePermission> OTHERS = EnumSet.of(PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_WRITE, PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE);

    private MountedSecrets() {
    }

    static void write(Path file, String value, String permissions) throws IOException {
        Files.writeString(file, value, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString(permissions));
    }

    static String permissions(Path file) throws IOException {
        return PosixFilePermissions.toString(Files.getPosixFilePermissions(file));
    }

    static Secret read(Path file) throws IOException, UnsafeFileException {
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file);
        if (permissions.stream().anyMatch(OTHERS::contains)) {
            throw new UnsafeFileException(file.getFileName() + " is readable by others (" + PosixFilePermissions.toString(permissions) + ")");
        }
        byte[] bytes = Files.readAllBytes(file);
        try {
            int end = bytes.length;
            while (end > 0 && (bytes[end - 1] == '\n' || bytes[end - 1] == '\r')) {
                end--;
            }
            CharBuffer chars = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes, 0, end));
            char[] value = new char[chars.remaining()];
            chars.get(value);
            Arrays.fill(chars.array(), '\0');
            return Secret.of(value);
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }
}
