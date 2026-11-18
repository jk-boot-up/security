package com.jk.explore.security.mtls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.junit.jupiter.api.Test;

/** The chain check by hand against the JDK's own check, on a fixed chain made once with step (root, intermediate and a payments certificate). */
class ChainStepsTest {

    private static List<X509Certificate> read(String name) throws Exception {
        return Credential.readCertificates(Path.of("src", "test", "resources", "chain", name));
    }

    private static boolean jdkTrusts(List<X509Certificate> chain, List<X509Certificate> roots) throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        for (int i = 0; i < roots.size(); i++) {
            store.setCertificateEntry("root" + i, roots.get(i));
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(store);
        try {
            ((X509TrustManager) tmf.getTrustManagers()[0]).checkClientTrusted(chain.toArray(new X509Certificate[0]), "EC");
            return true;
        } catch (CertificateException e) {
            return false;
        }
    }

    @Test
    void eachLinkNamesAndIsSignedByTheNext() throws Exception {
        List<ChainSteps.Link> links = ChainSteps.walk(read("payments-chain.crt"), read("root.crt"));
        assertEquals(2, links.size());
        assertEquals(new ChainSteps.Link("svc-payments-7", "Test Bank Intermediate CA", "Test Bank Intermediate CA", true), links.get(0));
        assertEquals(new ChainSteps.Link("Test Bank Intermediate CA", "Test Bank Root CA", "Test Bank Root CA", true), links.get(1));
    }

    @Test
    void byHandAndTheJdkAgree() throws Exception {
        List<X509Certificate> chain = read("payments-chain.crt");
        assertTrue(ChainSteps.trusted(chain, read("root.crt")));
        assertTrue(jdkTrusts(chain, read("root.crt")));
        assertFalse(ChainSteps.trusted(chain, read("other-root.crt")));
        assertFalse(jdkTrusts(chain, read("other-root.crt")));
    }

    @Test
    void aChainWithoutItsIntermediateIsNotTrusted() throws Exception {
        List<X509Certificate> leafOnly = List.of(read("payments-chain.crt").getFirst());
        assertFalse(ChainSteps.trusted(leafOnly, read("root.crt")));
        assertFalse(jdkTrusts(leafOnly, read("root.crt")));
    }

    @Test
    void theSanIsListedAndTheIdentityTakenFromIt() throws Exception {
        X509Certificate leaf = read("payments-chain.crt").getFirst();
        assertEquals(List.of("URI spiffe://bank/payments"), Identities.names(leaf));
        assertEquals("payments", Identities.of(leaf).orElseThrow());
        assertEquals("svc-payments-7", ChainSteps.cn(leaf.getSubjectX500Principal().getName()));
    }

    @Test
    void eachToolHasAPinnedAssetForEachMachine() {
        assertEquals("step_darwin_0.31.0_arm64.tar.gz", StepTools.asset("step", StepTools.STEP_VERSION, "Mac OS X", "aarch64"));
        assertEquals("step-ca_linux_0.30.2_amd64.tar.gz", StepTools.asset("step-ca", StepTools.STEP_CA_VERSION, "Linux", "amd64"));
    }
}
