package com.jk.explore.security.iammodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The evaluation rules, one at a time. */
class EvaluatorTest {

    private static final String A = "111111111111";
    private static final String B = "222222222222";
    private static final String OBJ = "arn:aws:s3:::bucket/key";

    private static Policy p(String json) {
        return Policy.parse("p", json);
    }

    private static boolean ok(Principal who, String action, String resource, Evaluator.Guard g) {
        return Evaluator.decide(new Request(who, action, resource), g).allowed();
    }

    @Test
    void nothingIsAllowedByDefault() {
        assertFalse(ok(Principal.user(A, "u"), "s3:GetObject", OBJ, Evaluator.Guard.none(A)));
    }

    @Test
    void anExplicitDenyBeatsAnyAllow() {
        Principal u = new Principal(A, "arn:aws:iam::" + A + ":user/u", List.of(Policy.of("a", true, "*", "*"), Policy.of("d", false, "s3:Delete*", "*")), null, null, Map.of());
        assertTrue(ok(u, "s3:GetObject", OBJ, Evaluator.Guard.none(A)));
        assertFalse(ok(u, "s3:DeleteObject", OBJ, Evaluator.Guard.none(A)));
    }

    @Test
    void wildcardsMatchActionsAnyCaseAndResourcesExactlyCased() {
        Principal u = Principal.user(A, "u", Policy.of("a", true, "s3:Get*", "arn:aws:s3:::bucket/*"));
        assertTrue(ok(u, "S3:getobject", OBJ, Evaluator.Guard.none(A)));
        assertFalse(ok(u, "s3:GetObject", "arn:aws:s3:::Bucket/key", Evaluator.Guard.none(A)));
        assertTrue(Wild.matches("a?c*", "abcdef", false));
        assertFalse(Wild.matches("a?c", "ac", false));
    }

    @Test
    void notActionAllowsEverythingElse() {
        Principal u = Principal.user(A, "u", p("{\"Statement\":[{\"Effect\":\"Allow\",\"NotAction\":\"iam:*\",\"Resource\":\"*\"}]}"));
        assertTrue(ok(u, "s3:DeleteBucket", OBJ, Evaluator.Guard.none(A)));
        assertFalse(ok(u, "iam:CreateUser", OBJ, Evaluator.Guard.none(A)));
    }

    @Test
    void withinOneAccountEitherSideIsEnoughButAcrossAccountsBothAreNeeded() {
        Policy resourceAllows = p("{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws:iam::" + A + ":root\"},\"Action\":\"s3:GetObject\",\"Resource\":\"*\"}]}");
        Principal nothing = Principal.user(A, "u", Policy.of("n", true, "x:y", "*"));
        Principal everything = Principal.user(A, "u", Policy.of("e", true, "s3:GetObject", "*"));
        assertTrue(ok(nothing, "s3:GetObject", OBJ, Evaluator.Guard.of(resourceAllows, A)));
        assertTrue(ok(everything, "s3:GetObject", OBJ, Evaluator.Guard.none(A)));
        assertFalse(ok(nothing, "s3:GetObject", OBJ, Evaluator.Guard.of(resourceAllows, B)));
        assertFalse(ok(everything, "s3:GetObject", OBJ, Evaluator.Guard.of(null, B)));
        assertTrue(ok(everything, "s3:GetObject", OBJ, Evaluator.Guard.of(resourceAllows, B)));
    }

    @Test
    void anonymousCallersNeedOnlyTheResourcePolicy() {
        Policy open = p("{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":\"s3:GetObject\",\"Resource\":\"*\"}]}");
        assertTrue(ok(Principal.anonymous(), "s3:GetObject", OBJ, Evaluator.Guard.of(open, A)));
        assertFalse(ok(Principal.anonymous(), "s3:PutObject", OBJ, Evaluator.Guard.of(open, A)));
        assertFalse(ok(Principal.user(B, "s", Policy.of("n", true, "x:y", "*")), "s3:GetObject", OBJ, Evaluator.Guard.of(open, A)));
    }

    @Test
    void organizationPoliciesMustAllowAndCanDeny() {
        Principal admin = Principal.user(A, "admin", Policy.of("all", true, "*", "*"));
        assertFalse(ok(admin, "s3:GetObject", OBJ, new Evaluator.Guard(null, A, List.of(Policy.of("ec2", true, "ec2:*", "*")))));
        assertTrue(ok(admin, "s3:GetObject", OBJ, new Evaluator.Guard(null, A, List.of(Policy.of("all", true, "*", "*")))));
        assertFalse(ok(admin, "s3:GetObject", OBJ, new Evaluator.Guard(null, A, List.of(Policy.of("all", true, "*", "*"), Policy.of("no-s3", false, "s3:*", "*")))));
    }

    @Test
    void boundaryAndSessionPolicyOnlyNarrow() {
        Principal wide = new Principal(A, "arn:aws:sts::" + A + ":assumed-role/R/s", List.of(Policy.of("all", true, "*", "*")), Policy.of("b", true, "s3:*", "*"), Policy.of("s", true, "s3:GetObject", "*"), Map.of());
        assertTrue(ok(wide, "s3:GetObject", OBJ, Evaluator.Guard.none(A)));
        assertFalse(ok(wide, "s3:PutObject", OBJ, Evaluator.Guard.none(A)));
        assertFalse(ok(wide, "ec2:RunInstances", OBJ, Evaluator.Guard.none(A)));
    }

    @Test
    void aResourcePolicyThatNamesThePrincipalIsNotNarrowedByTheBoundary() {
        String arn = "arn:aws:sts::" + A + ":assumed-role/R/s";
        Principal narrow = new Principal(A, arn, List.of(), Policy.of("b", true, "s3:GetObject", "*"), Policy.of("s", true, "s3:GetObject", "*"), Map.of());
        Policy names = p("{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"" + arn + "\"},\"Action\":\"s3:DeleteObject\",\"Resource\":\"*\"}]}");
        Policy accountOnly = p("{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws:iam::" + A + ":root\"},\"Action\":\"s3:DeleteObject\",\"Resource\":\"*\"}]}");
        assertTrue(ok(narrow, "s3:DeleteObject", OBJ, Evaluator.Guard.of(names, A)));
        assertFalse(ok(narrow, "s3:DeleteObject", OBJ, Evaluator.Guard.of(accountOnly, A)));
    }

    @Test
    void conditionsUseTheContextTagsAndPolicyVariables() {
        Principal u = Principal.user(A, "u", p("{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"s3:*\",\"Resource\":\"arn:aws:s3:::home/${aws:username}/*\","
                + "\"Condition\":{\"IpAddress\":{\"aws:SourceIp\":\"10.0.0.0/8\"},\"StringEquals\":{\"aws:PrincipalTag/team\":\"pay\"}}}]}")).withTags(Map.of("team", "pay"));
        Request mine = new Request(u, "s3:GetObject", "arn:aws:s3:::home/u/f", Map.of("aws:SourceIp", "10.1.2.3"));
        assertTrue(Evaluator.decide(mine, Evaluator.Guard.none(A)).allowed());
        assertFalse(Evaluator.decide(new Request(u, "s3:GetObject", "arn:aws:s3:::home/other/f", Map.of("aws:SourceIp", "10.1.2.3")), Evaluator.Guard.none(A)).allowed());
        assertFalse(Evaluator.decide(new Request(u, "s3:GetObject", "arn:aws:s3:::home/u/f", Map.of("aws:SourceIp", "8.8.8.8")), Evaluator.Guard.none(A)).allowed());
        assertFalse(Evaluator.decide(new Request(u, "s3:GetObject", "arn:aws:s3:::home/u/f"), Evaluator.Guard.none(A)).allowed());
    }

    @Test
    void missingKeysFailPositiveOperatorsAndPassNegatedOnesAndIfExists() {
        Map<String, Map<String, List<String>>> eq = Map.of("StringEquals", Map.of("k", List.of("v")));
        Map<String, Map<String, List<String>>> ne = Map.of("StringNotEquals", Map.of("k", List.of("v")));
        Map<String, Map<String, List<String>>> ie = Map.of("StringEqualsIfExists", Map.of("k", List.of("v")));
        Map<String, Map<String, List<String>>> nul = Map.of("Null", Map.of("k", List.of("true")));
        assertFalse(Conditions.holds(eq, Map.of()));
        assertTrue(Conditions.holds(ne, Map.of()));
        assertTrue(Conditions.holds(ie, Map.of()));
        assertFalse(Conditions.holds(ie, Map.of("k", "w")));
        assertTrue(Conditions.holds(nul, Map.of()));
        assertTrue(Conditions.inCidr("192.168.1.77", "192.168.1.0/24"));
        assertFalse(Conditions.inCidr("192.168.2.77", "192.168.1.0/24"));
    }

    @Test
    void allowedListsTheActionsThatPass() {
        Principal u = Principal.user(A, "u", Policy.of("a", true, "s3:Get*", "*"));
        assertEquals(List.of("s3:GetObject"), Evaluator.allowed(u, List.of("s3:GetObject", "s3:PutObject"), OBJ, Evaluator.Guard.none(A)));
    }
}
