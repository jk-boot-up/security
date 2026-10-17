package com.jk.explore.security.certs;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/** Changes one letter inside the signed part of a certificate, as an attacker or a bit error would. */
final class Tamper {

    private Tamper() {
    }

    /** The first occurrence of the server name is the subject's common name; its last letter is changed from lower to upper case. */
    static X509Certificate changeName(X509Certificate certificate) {
        try {
            byte[] bytes = certificate.getEncoded();
            byte[] needle = Pki.HOST.getBytes(StandardCharsets.US_ASCII);
            int at = indexOf(bytes, needle);
            if (at < 0) {
                throw new IllegalArgumentException("the name is not in the certificate");
            }
            bytes[at + needle.length - 1] -= 32;
            return CertBuilder.parse(bytes);
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int indexOf(byte[] bytes, byte[] needle) {
        for (int i = 0; i + needle.length <= bytes.length; i++) {
            int j = 0;
            while (j < needle.length && bytes[i + j] == needle[j]) {
                j++;
            }
            if (j == needle.length) {
                return i;
            }
        }
        return -1;
    }
}
