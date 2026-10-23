package com.jk.explore.security.securepipe;

import java.nio.file.Path;

/** The four files a release is made of: the jar, its signature, the provenance and the signature of the provenance. */
record Bundle(Path dir) {

    static final String JAR = "bank-ledger.jar";

    Path jar() {
        return dir.resolve(JAR);
    }

    Path jarSignature() {
        return dir.resolve(JAR + ".sig");
    }

    Path provenance() {
        return dir.resolve("provenance.json");
    }

    Path provenanceSignature() {
        return dir.resolve("provenance.json.sig");
    }
}
