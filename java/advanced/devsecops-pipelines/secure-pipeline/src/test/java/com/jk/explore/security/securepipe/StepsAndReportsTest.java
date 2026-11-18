package com.jk.explore.security.securepipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StepsAndReportsTest {

    @TempDir
    Path dir;

    @Test
    void theJdkStepsAcceptAnEcdsaSignatureAndRefuseAChangedFile() throws Exception {
        JdkSignatures.Pair pair = JdkSignatures.newPair(dir, "k");
        Path pem = dir.resolve("cosign.pub");
        Files.writeString(pem, "-----BEGIN PUBLIC KEY-----\n" + Base64.getMimeEncoder().encodeToString(Base64.getDecoder().decode(Files.readString(pair.publicKeyFile())))
                + "\n-----END PUBLIC KEY-----\n");
        Path file = dir.resolve("bank-ledger.jar");
        Files.writeString(file, "jar bytes");
        Path sig = dir.resolve("bank-ledger.jar.sig");
        JdkSignatures.sign(pair.signing(), file, sig);
        SignatureSteps steps = new SignatureSteps();
        assertTrue(steps.verify(pem, file, sig));
        assertEquals(new JdkSignatures().verify(pair.publicKeyFile(), file, sig), steps.verify(pem, file, sig));
        assertEquals("EC, 256 bits", SignatureSteps.describe(pem));
        Files.writeString(file, "jar bytez");
        assertFalse(steps.verify(pem, file, sig));
    }

    @Test
    void gitleaksFindingsAreRuleFileAndLine() throws Exception {
        Path report = dir.resolve("g.json");
        Files.writeString(report, "[{\"RuleID\":\"aws-access-token\",\"File\":\"config/payments.properties\",\"StartLine\":2}]");
        assertEquals(List.of("aws-access-token in config/payments.properties:2"), Reports.gitleaks(report));
        Files.writeString(report, "");
        assertEquals(List.of(), Reports.gitleaks(report));
    }

    @Test
    void trivyFindingsAreIdSeverityAndPackage() throws Exception {
        Path report = dir.resolve("t.json");
        Files.writeString(report, """
                {"Results":[{"Vulnerabilities":[
                  {"VulnerabilityID":"CVE-2022-42889","Severity":"CRITICAL","PkgName":"org.apache.commons:commons-text","InstalledVersion":"1.9"}]}]}
                """);
        assertEquals(List.of("CVE-2022-42889 CRITICAL org.apache.commons:commons-text 1.9"), Reports.trivy(report));
    }

    @Test
    void failedTestsComeFromTheJunitXml() throws Exception {
        Files.writeString(dir.resolve("TEST-bank.ledger.LedgerTest.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="bank.ledger.LedgerTest" tests="2" failures="1">
                  <testcase name="feeIsOnePercentRoundedDown()"><failure message="expected: &lt;100&gt; but was: &lt;1000&gt;">trace</failure></testcase>
                  <testcase name="accountCheckIsFourHexDigits()"/>
                </testsuite>
                """);
        assertEquals(List.of("feeIsOnePercentRoundedDown(): expected: <100> but was: <1000>"), Reports.junit(dir));
    }

    @Test
    void anAnswerSaysItsExitCode() {
        assertEquals("exit 7", new Gate.Answer(Gate.Outcome.FAIL, 7).exitText());
        assertEquals("no program", Gate.run(dir, List.of("no-such-tool-" + "anywhere"), 7).exitText());
        assertEquals(Gate.Outcome.ERROR, Gate.outcome(1, 3));
    }

    @Test
    void theVerdictCountsTheChecksPassedBeforeIt() {
        assertEquals(7, Verifier.Verdict.yes().checksPassed());
        assertEquals(1, Verifier.Verdict.no("artifact signature").checksPassed());
        assertEquals(5, Verifier.Verdict.no("source commit").checksPassed());
    }

    @Test
    void eachToolHasAPinnedAssetForEachMachine() {
        assertEquals("gitleaks_8.30.1_darwin_arm64.tar.gz", Tools.gitleaksAsset(true, true));
        assertEquals("trivy_0.76.1_Linux-64bit.tar.gz", Tools.trivyAsset(false, false));
        assertEquals("cosign-linux-amd64", Tools.cosignAsset(false, false));
    }
}
