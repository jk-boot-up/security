package com.jk.explore.security.linuxhard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads and sets the Unix permission bits of a file, including the set-user-id, set-group-id and sticky bits that the POSIX permission set leaves out. */
final class Modes {

    static final int SETUID = 04000;
    static final int SETGID = 02000;
    static final int STICKY = 01000;
    static final int OTHER_WRITE = 0002;
    static final int GROUP_WRITE = 0020;

    private Modes() {
    }

    static int of(Path p) throws IOException {
        return ((Integer) Files.getAttribute(p, "unix:mode")) & 07777;
    }

    static void set(Path p, int mode) throws IOException {
        Files.setAttribute(p, "unix:mode", mode);
    }

    static String octal(int mode) {
        return String.format("%04o", mode);
    }
}
