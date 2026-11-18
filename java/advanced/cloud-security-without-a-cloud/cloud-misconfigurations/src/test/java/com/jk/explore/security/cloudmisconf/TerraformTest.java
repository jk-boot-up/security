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
    void theFilesDeclareNineAndThirteenResources() {
        assertEquals(9, Terraform.resources(Terraform.FIRST));
        assertEquals(13, Terraform.resources(Terraform.FIXED));
    }

    @Test
    void aReportNamesTheFirstCheckThatFiredOrADash() {
        IacScanners.Report r = new IacScanners.Report(List.of("CKV_AWS_1 aws_s3_bucket.a", "CKV_AWS_24 aws_security_group.web"));
        assertEquals("CKV_AWS_24", r.first(List.of("CKV_AWS_24", "CKV_AWS_25")));
        assertEquals("-", r.first(List.of()));
        assertTrue(r.caughtOn(List.of("CKV_AWS_24"), "aws_security_group.web"));
        assertFalse(r.caughtOn(List.of("CKV_AWS_24"), "aws_security_group.database"));
        assertEquals(1, r.otherThan(List.of(List.of("CKV_AWS_24"))));
        assertEquals(Set.of("CKV_AWS_1", "CKV_AWS_24"), r.ids());
    }

    @Test
    void everyRuleHasAMappingForBothTools() {
        assertEquals(IacScanners.CHECKOV.keySet(), IacScanners.TRIVY.keySet());
        assertEquals(Set.copyOf(IacScanners.RULES), IacScanners.CHECKOV.keySet());
        assertEquals(9, IacScanners.RULES.size());
    }
}
