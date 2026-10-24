package com.jk.explore.security.stepca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Reading certificates and deciding a chain against a trust anchor is pure JDK work; only issuing from step-ca needs the tool (that is integration-tagged). */
class PkiTest {

    // A self-signed root (CN=Shop Lab Root) and a leaf it signed (CN=shop.example.test), both valid for a century.
    private static final String ROOT = """
            -----BEGIN CERTIFICATE-----
            MIIBZjCCAQygAwIBAgIUXgzseok3erZrF0AFwHvRlgZXWqcwCgYIKoZIzj0EAwIw
            GDEWMBQGA1UEAwwNU2hvcCBMYWIgUm9vdDAgFw0yNjEwMjQwODU2NDlaGA8yMTI2
            MDkzMDA4NTY0OVowGDEWMBQGA1UEAwwNU2hvcCBMYWIgUm9vdDBZMBMGByqGSM49
            AgEGCCqGSM49AwEHA0IABEDOxELB47CiZvYRejZMJMS929YCOTy0Hkd2AD2wQOX/
            Bb9BsWdO4vnJqraoQTO73N/PKfyLnvzdZNyHgLK6VMmjMjAwMB0GA1UdDgQWBBQ5
            miNQ40WBcWbkZLO+eK5TWBmd1jAPBgNVHRMBAf8EBTADAQH/MAoGCCqGSM49BAMC
            A0gAMEUCIQC5UmZ+JktwYDCAVIhBouMmQeoydhVH28xKUdmILoMjnAIgciOFiIgt
            y930H29yBOnibXTmZ1lz26MCqn/B66yYUPw=
            -----END CERTIFICATE-----
            """;
    private static final String LEAF = """
            -----BEGIN CERTIFICATE-----
            MIIBVzCB/qADAgECAhRQ1LGPH3vPQpcUdrd7EOXlxLZu3jAKBggqhkjOPQQDAjAY
            MRYwFAYDVQQDDA1TaG9wIExhYiBSb290MCAXDTI2MTAyNDA4NTY0OVoYDzIxMjYw
            OTMwMDg1NjQ5WjAcMRowGAYDVQQDDBFzaG9wLmV4YW1wbGUudGVzdDBZMBMGByqG
            SM49AgEGCCqGSM49AwEHA0IABLtYruUrBL6+ZoWDoH5A+4aBAP+uwwuJrgtN7aec
            CvQBvcf0JY+bkoZvp7n+REfJVvlOkaCMs0qnBcFEfctt0wyjIDAeMBwGA1UdEQQV
            MBOCEXNob3AuZXhhbXBsZS50ZXN0MAoGCCqGSM49BAMCA0gAMEUCIHzrfzLP4MKI
            VPuKBciL9fobOurx5ebybFW6sENQVUUhAiEAqhcVPjMSNpy1wtgHKvJV7Es1q50c
            M454XrI0PLFPQ68=
            -----END CERTIFICATE-----
            """;

    @Test
    void readsaCertificateAndItsSubject(@TempDir Path dir) throws Exception {
        Path leaf = Files.writeString(dir.resolve("leaf.crt"), LEAF);
        assertEquals(1, Pki.certificates(leaf).size());
        assertTrue(Pki.first(leaf).getSubjectX500Principal().getName().contains("shop.example.test"));
    }

    @Test
    void acceptsaLeafThatChainsToTheTrustedRoot(@TempDir Path dir) throws Exception {
        Path leaf = Files.writeString(dir.resolve("leaf.crt"), LEAF);
        Path root = Files.writeString(dir.resolve("root.crt"), ROOT);
        assertEquals("accepted", Pki.validate(leaf, root, null, false, new Date()));
    }

    @Test
    void refusesWhenTheCertificateIsNotYetValidAtTheGivenTime(@TempDir Path dir) throws Exception {
        Path leaf = Files.writeString(dir.resolve("leaf.crt"), LEAF);
        Path root = Files.writeString(dir.resolve("root.crt"), ROOT);
        assertTrue(Pki.validate(leaf, root, null, false, new Date(0L)).startsWith("refused"), "1970 is before the certificate's notBefore");
    }
}
