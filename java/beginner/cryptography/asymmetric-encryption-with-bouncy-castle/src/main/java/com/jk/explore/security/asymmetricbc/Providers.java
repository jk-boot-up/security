package com.jk.explore.security.asymmetricbc;

import java.security.Provider;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Registers Bouncy Castle as a security provider. Registration changes the whole JVM: every library in the process can
 * now be handed algorithms from Bouncy Castle, so it is done once, on purpose, and named explicitly where it is used.
 */
final class Providers {

    static final Provider BC = new BouncyCastleProvider();

    static {
        if (Security.getProvider(BC.getName()) == null) {
            Security.addProvider(BC);
        }
    }

    private Providers() {
    }

    /** Forces the registration above to run. */
    static void ensure() {
        // the static initialiser does the work
    }
}
