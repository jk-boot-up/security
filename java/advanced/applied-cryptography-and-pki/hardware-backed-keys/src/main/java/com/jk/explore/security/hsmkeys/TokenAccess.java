package com.jk.explore.security.hsmkeys;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.AuthProvider;

/** Logging in to the token through the JDK provider, and signing and verifying with the result. */
final class TokenAccess {

    enum LoginResult { OK, NO_PIN_GIVEN, PIN_INCORRECT, PIN_LOCKED, OTHER }

    record Login(LoginResult result, KeyStore store) {
    }

    private TokenAccess() {
    }

    /** Loads the PKCS11 KeyStore; a PIN of null means "use the login that already exists in this process". */
    static Login login(Provider provider, String pin) {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS11", provider);
            ks.load(null, pin == null ? null : pin.toCharArray());
            return new Login(LoginResult.OK, ks);
        } catch (IOException | GeneralSecurityException e) {
            return new Login(classify(e), null);
        }
    }

    static LoginResult classify(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            String m = String.valueOf(c.getMessage());
            if (m.contains("CKR_PIN_INCORRECT")) {
                return LoginResult.PIN_INCORRECT;
            }
            if (m.contains("CKR_PIN_LOCKED")) {
                return LoginResult.PIN_LOCKED;
            }
            if (m.contains("no password provided")) {
                return LoginResult.NO_PIN_GIVEN;
            }
        }
        return LoginResult.OTHER;
    }

    static void logout(Provider provider) throws GeneralSecurityException {
        ((AuthProvider) provider).logout();
    }

    static byte[] sign(Provider provider, PrivateKey key, byte[] data) throws GeneralSecurityException {
        Signature s = Signature.getInstance("SHA256withRSA", provider);
        s.initSign(key);
        s.update(data);
        return s.sign();
    }

    static boolean verify(PublicKey key, byte[] data, byte[] signature) {
        try {
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initVerify(key);
            s.update(data);
            return s.verify(signature);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }
}
