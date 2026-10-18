package com.jk.explore.security.signing;

import java.nio.charset.StandardCharsets;

/** A release file: its name and its exact bytes. */
record Artifact(String fileName, byte[] bytes) {

    /** The store's release jar for a version: fixed bytes, so its digest is the same on every run. */
    static Artifact shopApp(String version) {
        StringBuilder content = new StringBuilder("PK shop-app ").append(version).append('\n');
        for (int i = 0; i < 64; i++) {
            content.append("com/shop/Checkout").append(i).append(".class build=").append(version).append('\n');
        }
        return new Artifact("shop-app-" + version + ".jar", content.toString().getBytes(StandardCharsets.UTF_8));
    }

    byte[] sha256() {
        return Digests.sha256(bytes);
    }

    /** The same file with one byte changed, as a tampered mirror or a man in the middle would serve it. */
    Artifact withByteChanged(int position) {
        byte[] changed = bytes.clone();
        changed[position] ^= 0x01;
        return new Artifact(fileName, changed);
    }
}
