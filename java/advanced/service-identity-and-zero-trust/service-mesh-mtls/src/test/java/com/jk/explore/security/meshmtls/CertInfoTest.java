package com.jk.explore.security.meshmtls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CertInfoTest {

    private static final String PRINTED = """

            POD payments-647d84884-77tfk (1 of 1)

            Certificate:
                Data:
                    Version: 3 (0x2)
                    Serial Number: 4 (0x4)
                    Issuer: CN=identity.linkerd.
                    Validity
                        Not Before: Oct 23 09:06:52 2026 UTC
                        Not After : Oct 24 09:07:32 2026 UTC
                    Subject: CN=payments.bank.serviceaccount.identity.linkerd.cluster.local
            """;

    @Test
    void readsTheSubjectAndTheValidityOfWhatLinkerdIdentityPrints() {
        CertInfo c = CertInfo.fromIdentityOutput(PRINTED);
        assertEquals("payments.bank.serviceaccount.identity.linkerd.cluster.local", c.subject());
        assertEquals(Duration.ofHours(24), c.lifetime());
    }

    @Test
    void readsAPublicCertificateFromPem(@TempDir Path dir) throws Exception {
        Path keystore = dir.resolve("ks.p12");
        Path keytool = Path.of(System.getProperty("java.home"), "bin", "keytool");
        Sh.must(null, java.util.Map.of(), List.of(keytool.toString(), "-genkeypair", "-alias", "t", "-keyalg", "EC", "-dname", "CN=test.example",
                "-validity", "30", "-keystore", keystore.toString(), "-storepass", "changeit", "-storetype", "PKCS12"));
        String pem = Sh.must(null, java.util.Map.of(), List.of(keytool.toString(), "-exportcert", "-rfc", "-alias", "t", "-keystore", keystore.toString(),
                "-storepass", "changeit", "-storetype", "PKCS12"));
        CertInfo c = CertInfo.fromPem(pem);
        assertEquals("CN=test.example", c.subject());
        assertEquals(30, Duration.between(c.notBefore(), c.notAfter()).toDays());
        Files.deleteIfExists(keystore);
    }
}
