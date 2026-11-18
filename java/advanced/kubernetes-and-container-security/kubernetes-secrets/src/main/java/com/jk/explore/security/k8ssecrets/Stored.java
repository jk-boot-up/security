package com.jk.explore.security.k8ssecrets;

import java.util.ArrayList;
import java.util.List;

/** A value read straight out of etcd: whether it is encrypted, and whether a given text can be found in it. */
record Stored(String raw) {

    boolean absent() {
        return raw.isBlank();
    }

    boolean encrypted() {
        return raw.contains(Encryption.MARKER);
    }

    boolean contains(String text) {
        return raw.contains(text);
    }

    /** The first characters, with every byte that is not a printable letter shown as a dot. */
    String start(int n) {
        StringBuilder s = new StringBuilder();
        for (char c : raw.substring(0, Math.min(n, raw.length())).toCharArray()) {
            s.append(c >= 0x20 && c < 0x7f ? c : '.');
        }
        return s.toString();
    }

    /** The {@code n} bytes in front of a text, in hex: in a plain value they say which field follows and how long it is. */
    List<Integer> bytesBefore(String text, int n) {
        int at = raw.indexOf(text);
        List<Integer> bytes = new ArrayList<>();
        for (int i = at - n; i < at; i++) {
            bytes.add((int) raw.charAt(i));
        }
        return bytes;
    }

    /** The part of an encrypted value in front of the ciphertext, split at the colons: k8s:enc, aescbc, v1, key1. */
    List<String> markerParts() {
        if (!encrypted()) {
            return List.of();
        }
        int at = raw.indexOf(Encryption.MARKER);
        String[] p = raw.substring(at, at + Encryption.MARKER.length()).split(":");
        return List.of(p[0] + ":" + p[1], p[2], p[3], p[4]);
    }
}
