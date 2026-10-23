package com.jk.explore.security.k8ssecrets;

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
}
