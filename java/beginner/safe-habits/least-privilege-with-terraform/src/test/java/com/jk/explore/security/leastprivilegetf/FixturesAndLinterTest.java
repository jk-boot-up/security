package com.jk.explore.security.leastprivilegetf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FixturesAndLinterTest {

    @Test
    void everyFixtureParsesAndHasAVersion() {
        for (String name : PolicyNames.ALL) {
            assertTrue(Fixtures.json(name).contains("\"Version\": \"2012-10-17\""), name);
            assertFalse(Fixtures.policy(name).statements().isEmpty(), name);
        }
    }

    @Test
    void blastRadiusOfEachPolicy() {
        assertEquals(25, Catalogue.all().size());
        assertEquals(25, Catalogue.blastRadius(Evaluator.of(Fixtures.policy("wildcard"))));
        assertEquals(3, Catalogue.blastRadius(Evaluator.of(Fixtures.policy("exporter"))));
        assertEquals(20, Catalogue.blastRadius(Evaluator.of(Fixtures.policy("admin_with_deny"))));
        assertEquals(3, Catalogue.blastRadius(Evaluator.of(Fixtures.policy("deployer"))));
    }

    @Test
    void theBoundaryShrinksTheWildcardToTheReadsAndNothingSecret() {
        Evaluator bounded = Evaluator.of(Fixtures.policy("wildcard")).withBoundary(Fixtures.policy("boundary"));
        assertEquals(8, Catalogue.blastRadius(bounded));
        assertTrue(Catalogue.reachable(bounded).stream().noneMatch(p -> p.resource().contains("shop-secrets")));
        assertTrue(Catalogue.reachable(bounded).stream().noneMatch(p -> p.action().startsWith("iam:")));
    }

    @Test
    void exporterNeedsTheBucketAndTheContentsSeparately() {
        Evaluator exporter = Evaluator.of(Fixtures.policy("exporter"));
        assertTrue(exporter.allows("s3:ListBucket", Catalogue.REPORTS));
        assertFalse(exporter.allows("s3:ListBucket", Catalogue.REPORT_FILE));
        assertFalse(exporter.allows("s3:GetObject", Catalogue.REPORTS));
        assertTrue(exporter.allows("s3:GetObject", Catalogue.REPORT_FILE));
    }

    @Test
    void linterFindsTheShortcutsAndTheEscalation() {
        assertEquals(0, Linter.check(Fixtures.policy("exporter")).size());
        assertEquals(1, Linter.count(Linter.check(Fixtures.policy("wildcard")), "ACTION_STAR"));
        assertEquals(1, Linter.count(Linter.check(Fixtures.policy("wildcard")), "RESOURCE_STAR"));
        assertEquals(1, Linter.count(Linter.check(Fixtures.policy("deployer")), "ESCALATION"));
    }

    @Test
    void linterFlagsServiceWildcardsAndNotAction() {
        var policy = PolicyDocument.parse("p", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"iam:*\",\"Resource\":\"arn:x\"},"
                + "{\"Effect\":\"Allow\",\"NotAction\":\"s3:*\",\"Resource\":\"arn:x\"}]}");
        var findings = Linter.check(policy);
        assertEquals(1, Linter.count(findings, "SERVICE_STAR"));
        assertEquals(1, Linter.count(findings, "NOT_ACTION"));
    }

    @Test
    void denyStatementsAreNotFlagged() {
        var policy = PolicyDocument.parse("p", "{\"Statement\":[{\"Effect\":\"Deny\",\"Action\":\"*\",\"Resource\":\"*\"}]}");
        assertEquals(0, Linter.check(policy).size());
    }
}
