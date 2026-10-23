package com.jk.explore.security.securepipe;

import java.nio.file.Path;

/** Says whether a signature file is a valid signature of a file under a public key. Cosign in the demo, the JDK in the unit tests. */
interface SignatureCheck {

    boolean verify(Path publicKey, Path file, Path signature);
}
