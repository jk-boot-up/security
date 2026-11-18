package com.jk.explore.security.certlife;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The chain steps done by hand accept and refuse the same chains as the JDK. The chain is made with the JDK's keytool: no other tool. */
class ChainStepsTest {

    @TempDir
    static Path dir;
    private static X509Certificate root;
    private static X509Certificate inter;
    private static X509Certificate leaf;

    private static void keytool(String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "keytool").toString()));
        cmd.addAll(List.of(args));
        cmd.addAll(List.of("-keystore", dir.resolve("ks.p12").toString(), "-storepass", "changeit"));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (p.waitFor() != 0) {
            throw new IllegalStateException(out);
        }
    }

    private static X509Certificate read(String file) throws Exception {
        try (var in = Files.newInputStream(dir.resolve(file))) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }

    @BeforeAll
    static void makeChain() throws Exception {
        for (String[] k : List.of(new String[] {"root", "O=Bank,CN=Test Root CA"}, new String[] {"inter", "O=Bank,CN=Test Issuing CA"},
                new String[] {"leaf", "O=Bank,CN=api.bank.example"})) {
            keytool("-genkeypair", "-alias", k[0], "-keyalg", "EC", "-groupname", "secp256r1", "-dname", k[1], "-validity", "3650",
                    "-ext", "bc:c", "-ext", "ku:c=keyCertSign,cRLSign");
        }
        keytool("-exportcert", "-rfc", "-alias", "root", "-file", dir.resolve("root.pem").toString());
        keytool("-certreq", "-alias", "inter", "-file", dir.resolve("inter.csr").toString());
        keytool("-gencert", "-alias", "root", "-infile", dir.resolve("inter.csr").toString(), "-outfile", dir.resolve("inter.pem").toString(),
                "-rfc", "-validity", "1825", "-ext", "bc:c=ca:true,pathlen:0", "-ext", "ku:c=keyCertSign,cRLSign");
        keytool("-certreq", "-alias", "leaf", "-file", dir.resolve("leaf.csr").toString());
        keytool("-gencert", "-alias", "inter", "-infile", dir.resolve("leaf.csr").toString(), "-outfile", dir.resolve("leaf.pem").toString(),
                "-rfc", "-validity", "30", "-ext", "ku:c=digitalSignature", "-ext", "eku=serverAuth", "-ext", "san=dns:api.bank.example");
        root = read("root.pem");
        inter = read("inter.pem");
        leaf = read("leaf.pem");
    }

    private static void sameAsTheJdk(List<X509Certificate> intermediates, Instant when) {
        List<ChainSteps.Step> steps = ChainSteps.check(leaf, intermediates, root, when);
        Verdict jdk = Chains.validate(leaf, intermediates, root, when, Chains.Source.NONE, Chains.Policy.HARD, List.of());
        assertEquals(jdk.accepted(), ChainSteps.accepted(steps), jdk.word() + " " + steps);
    }

    @Test
    void aGoodChainPassesEveryStep() {
        List<ChainSteps.Step> steps = ChainSteps.check(leaf, List.of(inter), root, Instant.now());
        assertEquals(7, steps.size());
        assertTrue(ChainSteps.accepted(steps));
        assertEquals(0, ChainSteps.firstFailure(steps));
        sameAsTheJdk(List.of(inter), Instant.now());
    }

    @Test
    void withoutTheIntermediateTheFirstStepFails() {
        List<ChainSteps.Step> steps = ChainSteps.check(leaf, List.of(), root, Instant.now());
        assertFalse(ChainSteps.accepted(steps));
        assertEquals(1, ChainSteps.firstFailure(steps));
        sameAsTheJdk(List.of(), Instant.now());
    }

    @Test
    void outsideTheDatesTheLastStepFails() {
        for (Instant when : List.of(Instant.now().plus(31, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS))) {
            List<ChainSteps.Step> steps = ChainSteps.check(leaf, List.of(inter), root, when);
            assertEquals(7, ChainSteps.firstFailure(steps));
            sameAsTheJdk(List.of(inter), when);
        }
    }

    @Test
    void theWrongRootFailsAtTheRootStep() {
        // the client holds the intermediate as its root: the intermediate's issuer is not that certificate
        List<ChainSteps.Step> steps = ChainSteps.check(leaf, List.of(inter), inter, Instant.now());
        assertFalse(ChainSteps.accepted(steps));
        assertEquals(3, ChainSteps.firstFailure(steps));
    }
}
