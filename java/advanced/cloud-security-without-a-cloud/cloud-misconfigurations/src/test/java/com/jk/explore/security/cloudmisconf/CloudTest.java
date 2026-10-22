package com.jk.explore.security.cloudmisconf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Starts a real Moto server and runs Checkov and Trivy: needs Python and Trivy (./gradlew integrationTest). */
@Tag("integration")
class CloudTest {

    @Test
    void theFirstVersionHasAllNineKindsOfProblem() throws Exception {
        try (Venv.Moto moto = new Venv.Moto(new Venv(Path.of("build")))) {
            Cloud cloud = new Cloud(moto.url());
            Environment.deployFirstVersion(cloud);
            List<Finding> findings = Scanner.scan(cloud);
            assertEquals(13, findings.size());
            Set<String> rules = findings.stream().map(Finding::rule).collect(Collectors.toSet());
            assertEquals(9, rules.size());
        }
    }

    @Test
    void theFixesLeaveNothing() throws Exception {
        try (Venv.Moto moto = new Venv.Moto(new Venv(Path.of("build")))) {
            Cloud cloud = new Cloud(moto.url());
            Environment.deployFirstVersion(cloud);
            Environment.harden(cloud);
            assertTrue(Scanner.scan(cloud).isEmpty());
        }
    }

    @Test
    void trivyMissesTheWildcardPolicyAndCheckovFindsIt() throws Exception {
        Venv venv = new Venv(Path.of("build"));
        Path dir = Terraform.write(Path.of("build/tf-test"), Terraform.FIRST);
        assertTrue(IacScanners.checkov(venv, dir).ids().contains("CKV_AWS_355"));
        assertTrue(IacScanners.catches(IacScanners.trivy(dir).ids(), IacScanners.TRIVY.get("iam-wildcard-admin")).isEmpty());
    }
}
