package com.jk.explore.security.tlsservers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The rules redone by hand, checked without any server: certificates made once with step are in src/test/resources/certs. */
class RulesByHandTest {

    private static X509Certificate cert(String name) throws Exception {
        return Https.certificates(Path.of("src/test/resources/certs", name)).get(0);
    }

    /** What the JDK's own validator says about the chain, on a fixed date inside the certificates' lifetime. */
    private static boolean jdkTrusts(X509Certificate leaf, X509Certificate root) throws Exception {
        PKIXParameters params = new PKIXParameters(Set.of(new TrustAnchor(root, null)));
        params.setRevocationEnabled(false);
        params.setDate(new Date(leaf.getNotBefore().getTime() + 3_600_000));
        try {
            CertPathValidator.getInstance("PKIX").validate(CertificateFactory.getInstance("X.509").generateCertPath(List.of(leaf)), params);
            return true;
        } catch (CertPathValidatorException e) {
            return false;
        }
    }

    @Test
    void theChainCheckByHandAgreesWithTheJdkValidator() throws Exception {
        X509Certificate www = cert("www.crt");
        for (X509Certificate root : List.of(cert("root.crt"), cert("other-root.crt"))) {
            assertEquals(jdkTrusts(www, root), ChainCheck.check(List.of(www), List.of(root), "www.shop.localhost").trusted());
        }
    }

    @Test
    void theChainCheckNamesTheSignerAndChecksTheName() throws Exception {
        X509Certificate www = cert("www.crt");
        ChainCheck.Verdict ok = ChainCheck.check(List.of(www), List.of(cert("root.crt")), "www.shop.localhost");
        assertEquals("Shop Lab Root", ok.signer());
        assertTrue(ok.ok());
        ChainCheck.Verdict otherName = ChainCheck.check(List.of(www), List.of(cert("root.crt")), "pay.shop.localhost");
        assertTrue(otherName.trusted());
        assertFalse(otherName.listed());
        assertFalse(ChainCheck.check(List.of(www), List.of(cert("other-root.crt")), "www.shop.localhost").ok());
        assertEquals(List.of("www.shop.localhost"), ChainCheck.names(www));
        assertFalse(ChainCheck.namesAResponder(www));
    }

    @Test
    void theLabRootIsNotAmongTheJdkDefaultRoots() throws Exception {
        assertFalse(ChainCheck.check(List.of(cert("www.crt")), ChainCheck.defaultRoots(), "www.shop.localhost").trusted());
    }

    @Test
    void theAgreedVersionIsTheHighestBothSpeak() {
        assertEquals(Optional.of("TLSv1.3"), Negotiation.agree(List.of("TLSv1.2", "TLSv1.3"), List.of("TLSv1.2", "TLSv1.3")));
        assertEquals(Optional.of("TLSv1.2"), Negotiation.agree(List.of("TLSv1.2", "TLSv1.3"), List.of("TLSv1.2")));
        assertEquals(Optional.empty(), Negotiation.agree(List.of("TLSv1.3"), List.of("TLSv1.2")));
    }

    @Test
    void aLocationWithItsOwnHeadersDropsTheServers() {
        assertEquals(List.of("Strict-Transport-Security"), Headers.effective(List.of("Strict-Transport-Security"), List.of()));
        assertEquals(List.of("X-Frame-Options"), Headers.effective(List.of("Strict-Transport-Security"), List.of("X-Frame-Options")));
    }

    @Test
    void maxAgeIsReadInSecondsAndIsAYear() {
        String value = "max-age=31536000; includeSubDomains";
        assertEquals(365, Headers.maxAge(value) / Headers.SECONDS_A_DAY);
        assertTrue(Headers.includesSubDomains(value));
        assertFalse(Headers.includesSubDomains("max-age=60"));
        assertThrows(IllegalArgumentException.class, () -> Headers.maxAge("includeSubDomains"));
    }

    @Test
    void theNginxConfigurationCarriesEachChoiceAsALine(@TempDir Path dir) throws Exception {
        Nginx nginx = new Nginx(dir, "www.crt", "www.key", "root.crt");
        String config = nginx.config(Nginx.Settings.defaults().protocols("TLSv1.3").serverTokens(false).hsts(true).accountHeader(true, false).stapling(true));
        assertTrue(config.contains("ssl_protocols TLSv1.3;"));
        assertTrue(config.contains("server_tokens off;"));
        assertTrue(config.contains(Nginx.HSTS));
        assertTrue(config.contains("ssl_stapling on;"));
        assertEquals(1, config.split("Strict-Transport-Security", -1).length - 1);
    }
}
