package com.jk.explore.security.tlscheck;

/** Classifies a cipher suite by its IANA name. The rules are the ones in the Mozilla server-side TLS guidance, kept small. */
public final class CipherSuites {

    private CipherSuites() {
    }

    /** NULL, export, anonymous, RC4, DES, 3DES and MD5 suites: broken, or open to known attacks. */
    public static boolean broken(String suite) {
        return suite.contains("NULL") || suite.contains("EXPORT") || suite.contains("_anon_") || suite.contains("RC4")
                || suite.contains("_DES_") || suite.contains("3DES") || suite.contains("MD5");
    }

    /** TLS 1.3 suites have no key exchange in their name and always give forward secrecy. */
    public static boolean tls13(String suite) {
        return suite.startsWith("TLS_AES_") || suite.startsWith("TLS_CHACHA20_");
    }

    /** True when a recorded session can be decrypted later if the server's key leaks. */
    public static boolean lacksForwardSecrecy(String suite) {
        return !tls13(suite) && !suite.contains("_ECDHE_") && !suite.contains("_DHE_");
    }

    /** CBC suites in TLS 1.2 are not broken, but need care against padding attacks; GCM and ChaCha20 are preferred. */
    public static boolean cbc(String suite) {
        return suite.contains("_CBC_");
    }
}
