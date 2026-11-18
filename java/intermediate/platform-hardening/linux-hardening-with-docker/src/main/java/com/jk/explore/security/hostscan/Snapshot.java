package com.jk.explore.security.hostscan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** What the scanning script printed, split at its #section lines, with the views a person reads first. */
record Snapshot(Map<String, List<String>> sections) {

    /** Splits the script's output at its #section lines. */
    static Snapshot of(String output) {
        Map<String, List<String>> out = new java.util.LinkedHashMap<>();
        String current = null;
        for (String line : output.split("\n")) {
            if (line.startsWith("#")) {
                current = line.substring(1);
                out.put(current, new ArrayList<>());
            } else if (current != null && !line.isBlank()) {
                out.get(current).add(line);
            }
        }
        return new Snapshot(out);
    }

    List<String> lines(String section) {
        return sections.getOrDefault(section, List.of());
    }

    /** The account lines of /etc/passwd, split into their seven fields. */
    List<String[]> accounts() {
        return lines("passwd").stream().map(l -> l.split(":", -1)).filter(f -> f.length >= 7).toList();
    }

    /** The second field of each /etc/shadow line: the password hash, empty for no password at all. */
    Map<String, String> passwordHashes() {
        Map<String, String> hashes = new HashMap<>();
        for (String line : lines("shadow")) {
            String[] f = line.split(":", -1);
            hashes.put(f[0], f.length > 1 ? f[1] : "!");
        }
        return hashes;
    }

    static boolean canLogIn(String[] account) {
        return !account[6].endsWith("nologin") && !account[6].endsWith("false");
    }

    List<String> uidZero() {
        return accounts().stream().filter(f -> f[2].equals("0")).map(f -> f[0]).toList();
    }

    List<String> emptyPasswords() {
        Map<String, String> hashes = passwordHashes();
        return accounts().stream().filter(f -> canLogIn(f) && hashes.getOrDefault(f[0], "!").isEmpty()).map(f -> f[0]).toList();
    }

    List<Listeners.Listener> listeners() {
        return Listeners.read(lines("tcp"));
    }
}
