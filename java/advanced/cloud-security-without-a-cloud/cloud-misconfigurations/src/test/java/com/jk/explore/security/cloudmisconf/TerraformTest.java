package com.jk.explore.security.cloudmisconf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TerraformTest {

    @Test
    void theFirstVersionOpensThePortsAndTheBuckets() {
        assertTrue(Terraform.FIRST.contains("0.0.0.0/0"));
        assertTrue(Terraform.FIRST.contains("public-read"));
        assertTrue(Terraform.FIRST.contains("\"optional\""));
    }

    @Test
    void theFixedVersionOpensNothingToTheWorld() {
        assertFalse(Terraform.FIXED.contains("0.0.0.0/0"));
        assertFalse(Terraform.FIXED.contains("public-read"));
        assertTrue(Terraform.FIXED.contains("\"required\""));
    }

    @Test
    void writeCreatesMainTf(@TempDir Path dir) throws Exception {
        Path out = Terraform.write(dir.resolve("a"), "x");
        assertEquals("x", Files.readString(out.resolve("main.tf")));
    }

    @Test
    void catchesReportsOnlyTheIdsThatFailed() {
        assertEquals(List.of("CKV_AWS_20"), IacScanners.catches(Set.of("CKV_AWS_20", "CKV_AWS_1"), List.of("CKV_AWS_20", "CKV_AWS_99")));
        assertTrue(IacScanners.catches(Set.of(), List.of("CKV_AWS_20")).isEmpty());
    }

    @Test
    void everyRuleHasAMappingForBothTools() {
        assertEquals(IacScanners.CHECKOV.keySet(), IacScanners.TRIVY.keySet());
        assertEquals(9, IacScanners.CHECKOV.size());
    }
}
