package com.jk.explore.security.tlsopenssl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;

/** PEM text, the format every other TLS program reads: a label line, the DER bytes in Base64 with 64 characters per line, a closing label line. */
final class Pem {

    private Pem() {
    }

    static String encode(String label, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        return "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----\n";
    }

    static String certificate(X509Certificate certificate) throws GeneralSecurityException {
        return encode("CERTIFICATE", certificate.getEncoded());
    }

    /** The private key as PKCS#8, in the clear. */
    static String privateKey(PrivateKey key) {
        return encode("PRIVATE KEY", key.getEncoded());
    }

    static void writeCertificate(Path file, X509Certificate certificate) throws GeneralSecurityException, IOException {
        Files.writeString(file, certificate(certificate));
    }

    /** Writes the private key readable by its owner only. */
    static void writePrivateKey(Path file, PrivateKey key) throws IOException {
        Files.deleteIfExists(file);
        Files.createFile(file, java.nio.file.attribute.PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        Files.writeString(file, privateKey(key));
    }
}
