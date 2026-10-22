package com.jk.explore.security.iacscan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ScannersTest {

    @Test
    void catchesLooksForAnyOfTheChecks() {
        assertTrue(Scanners.catches(Set.of("B"), java.util.List.of("A", "B")));
        assertFalse(Scanners.catches(Set.of("B"), java.util.List.of()));
    }

    @Test
    void everyRuleHasAMappingForEachTool() {
        assertEquals(8, Scanners.CHECKOV.size());
        assertEquals(Scanners.CHECKOV.keySet(), Scanners.TRIVY.keySet());
        assertEquals(Scanners.CHECKOV.keySet(), Scanners.KICS.keySet());
    }

    @Tag("integration")
    @Test
    void noScannerFindsThePasswordWrittenInTheFile() throws Exception {
        Path dir = Scanners.write(Path.of("build/scanners-test"), Manifests.FIRST);
        assertFalse(Scanners.checkov(new Venv(Path.of("build")), dir).contains("CKV_K8S_35"));
        assertTrue(Scanners.trivy(dir).contains("KSV-0017"));
        assertTrue(Scanners.kics(dir).contains("Container Is Privileged"));
    }
}
