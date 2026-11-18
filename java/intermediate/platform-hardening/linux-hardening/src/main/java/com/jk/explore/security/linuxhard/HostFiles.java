package com.jk.explore.security.linuxhard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the text files that describe a host's accounts, its SSH daemon and its kernel settings. A missing file reads as empty. */
final class HostFiles {

    private HostFiles() {
    }

    /** The lines of /etc/passwd, split into their seven fields: name, x, UID, GID, comment, home, shell. */
    static List<String[]> passwd(Path root) throws IOException {
        List<String[]> out = new ArrayList<>();
        for (String line : lines(root.resolve("etc/passwd"))) {
            String[] p = line.split(":", -1);
            if (p.length >= 7) {
                out.add(p);
            }
        }
        return out;
    }

    /** The second field of /etc/shadow for each account: the password hash, or empty for no password at all. */
    static Map<String, String> passwordHashes(Path root) throws IOException {
        Map<String, String> hashes = new HashMap<>();
        for (String line : lines(root.resolve("etc/shadow"))) {
            String[] p = line.split(":", -1);
            if (p.length > 1) {
                hashes.put(p[0], p[1]);
            }
        }
        return hashes;
    }

    /** True when the account's shell lets it log in. */
    static boolean canLogIn(String[] account) {
        return !(account[6].endsWith("nologin") || account[6].endsWith("false"));
    }

    /** sshd_config as lower-case keyword to lower-case value; the first value wins, as in sshd. */
    static Map<String, String> sshd(Path root) throws IOException {
        Map<String, String> s = new LinkedHashMap<>();
        for (String line : lines(root.resolve("etc/ssh/sshd_config"))) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#")) {
                String[] p = t.split("\\s+", 2);
                s.putIfAbsent(p[0].toLowerCase(), p.length > 1 ? p[1].trim().toLowerCase() : "");
            }
        }
        return s;
    }

    /** sysctl.conf as name to value. */
    static Map<String, String> sysctl(Path root) throws IOException {
        Map<String, String> s = new LinkedHashMap<>();
        for (String line : lines(root.resolve("etc/sysctl.conf"))) {
            String t = line.trim();
            if (t.contains("=") && !t.startsWith("#")) {
                s.put(t.substring(0, t.indexOf('=')).trim(), t.substring(t.indexOf('=') + 1).trim());
            }
        }
        return s;
    }

    private static List<String> lines(Path p) throws IOException {
        return Files.exists(p) ? Files.readAllLines(p) : List.of();
    }
}
