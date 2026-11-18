package com.jk.explore.security.linuxhard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    /** One permission digit as letters: 7 is rwx (read, write, execute), 5 is r-x, 4 is r--. */
    static String rwx(int digit) {
        return ((digit & 4) != 0 ? "r" : "-") + ((digit & 2) != 0 ? "w" : "-") + ((digit & 1) != 0 ? "x" : "-");
    }

    /** The first digit: 4 set-user-id, 2 set-group-id, 1 sticky, added together. */
    static String special(int digit) {
        List<String> names = new ArrayList<>();
        if ((digit & 4) != 0) {
            names.add("set-user-id");
        }
        if ((digit & 2) != 0) {
            names.add("set-group-id");
        }
        if ((digit & 1) != 0) {
            names.add("sticky");
        }
        return names.isEmpty() ? "none" : String.join(" + ", names);
    }
}
