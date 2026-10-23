package com.jk.explore.security.runtimesec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PodsAndRulesFileTest {

    @Test
    void theServiceIsStartedWithoutAShell() {
        String m = Pods.api("payments-api", false);
        assertTrue(m.contains("command: [\"httpd\""));
        assertTrue(!m.contains("\"sh\""));
    }

    @Test
    void theReadOnlyVariantDiffersOnlyInTheRootFileSystem() {
        assertTrue(Pods.api("payments-api", true).contains("readOnlyRootFilesystem: true"));
        assertTrue(Pods.api("payments-api", false).contains("readOnlyRootFilesystem: false"));
    }

    @Test
    void theLogFolderIsAVolumeSoItStaysWritableWhenTheRootIsNot() {
        assertTrue(Pods.api("payments-api", true).contains("mountPath: /var/log/payments"));
    }

    @Test
    void theLedgerHasAServiceInFrontOfIt() {
        assertTrue(Pods.ledger().contains("kind: Service"));
    }

    @Test
    void theFalcoRulesFileHoldsTheSameThreeRulesAsTheJavaTests() throws Exception {
        String yaml = Files.readString(Path.of("rules", "bank-runtime-rules.yaml"));
        assertEquals(3, Falco.countRules(yaml));
        assertTrue(yaml.contains("Shell in a bank container"));
        assertTrue(yaml.contains("Unexpected write in a bank container"));
        assertTrue(yaml.contains("Unexpected outbound connection from a bank container"));
        for (String prefix : new String[] {"/var/log/payments/", "/tmp/", "/dev/"}) {
            assertTrue(yaml.contains("startswith " + prefix), prefix);
        }
    }

    @Test
    void theCollectorDoesNotStartAProgramForEachRecord() throws Exception {
        String sh = Files.readString(Path.of("src", "main", "resources", "collector.sh"));
        String loop = sh.substring(sh.indexOf("read -r line; do"));
        assertTrue(!loop.contains("$(") && !loop.contains(" | "), "a program started in the loop is audited itself, without end");
    }
}
