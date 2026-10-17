package com.jk.explore.security.leastprivilegetf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EvaluatorTest {

    private static final String SHOP = "arn:aws:s3:::shop-reports";

    private static PolicyDocument policy(String statements) {
        return PolicyDocument.parse("p", "{\"Version\":\"2012-10-17\",\"Statement\":" + statements + "}");
    }

    @Test
    void nothingIsAllowedByDefault() {
        assertEquals(Evaluator.Decision.IMPLICIT_DENY, new Evaluator(List.of(), null).decide("s3:GetObject", SHOP));
    }

    @Test
    void anExplicitDenyBeatsAnAllowInAnyOrder() {
        PolicyDocument allow = policy("[{\"Effect\":\"Allow\",\"Action\":\"*\",\"Resource\":\"*\"}]");
        PolicyDocument deny = policy("{\"Effect\":\"Deny\",\"Action\":\"s3:*\",\"Resource\":\"*\"}");
        assertEquals(Evaluator.Decision.EXPLICIT_DENY, new Evaluator(List.of(allow, deny), null).decide("s3:GetObject", SHOP));
        assertEquals(Evaluator.Decision.EXPLICIT_DENY, new Evaluator(List.of(deny, allow), null).decide("s3:GetObject", SHOP));
        assertEquals(Evaluator.Decision.ALLOW, new Evaluator(List.of(deny, allow), null).decide("kms:Decrypt", SHOP));
    }

    @Test
    void aBoundaryOnlyNarrows() {
        PolicyDocument all = policy("[{\"Effect\":\"Allow\",\"Action\":\"*\",\"Resource\":\"*\"}]");
        PolicyDocument reads = policy("[{\"Effect\":\"Allow\",\"Action\":\"s3:Get*\",\"Resource\":\"*\"}]");
        Evaluator bounded = Evaluator.of(all).withBoundary(reads);
        assertTrue(bounded.allows("s3:GetObject", SHOP));
        assertEquals(Evaluator.Decision.IMPLICIT_DENY, bounded.decide("s3:PutObject", SHOP));
        assertEquals(Evaluator.Decision.IMPLICIT_DENY, new Evaluator(List.of(), reads).decide("s3:GetObject", SHOP));
    }

    @Test
    void aDenyInTheBoundaryWins() {
        PolicyDocument all = policy("[{\"Effect\":\"Allow\",\"Action\":\"*\",\"Resource\":\"*\"}]");
        PolicyDocument boundary = policy("[{\"Effect\":\"Allow\",\"Action\":\"*\",\"Resource\":\"*\"},{\"Effect\":\"Deny\",\"Action\":\"iam:*\",\"Resource\":\"*\"}]");
        assertEquals(Evaluator.Decision.EXPLICIT_DENY, Evaluator.of(all).withBoundary(boundary).decide("iam:CreateUser", SHOP));
    }

    @Test
    void notActionAllowsEverythingElse() {
        PolicyDocument notIam = policy("[{\"Effect\":\"Allow\",\"NotAction\":\"iam:*\",\"Resource\":\"*\"}]");
        Evaluator evaluator = Evaluator.of(notIam);
        assertTrue(evaluator.allows("s3:GetObject", SHOP));
        assertEquals(Evaluator.Decision.IMPLICIT_DENY, evaluator.decide("iam:CreateUser", SHOP));
    }

    @Test
    void conditionsAndOddEffectsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> policy("[{\"Effect\":\"Allow\",\"Action\":\"*\",\"Resource\":\"*\",\"Condition\":{}}]"));
        assertThrows(IllegalArgumentException.class, () -> policy("[{\"Effect\":\"Maybe\",\"Action\":\"*\",\"Resource\":\"*\"}]"));
        assertThrows(IllegalArgumentException.class, () -> PolicyDocument.parse("p", "{\"Version\":\"2012-10-17\"}"));
    }

    @Test
    void aSingleStatementObjectIsAccepted() {
        assertEquals(1, policy("{\"Effect\":\"Allow\",\"Action\":[\"s3:GetObject\"],\"Resource\":\"*\"}").statements().size());
    }
}
