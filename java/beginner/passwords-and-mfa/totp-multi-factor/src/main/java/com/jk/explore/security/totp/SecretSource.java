package com.jk.explore.security.totp;

/** Where shared secrets and recovery codes come from. */
public interface SecretSource {

    /** A new 20-byte secret key. */
    byte[] newSecret();

    /** A new recovery code. */
    String newRecoveryCode();
}
