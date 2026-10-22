package com.jk.explore.security.iammodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Trust policies, sessions, the confused deputy and the escalation search. */
class RoleTest {

    private static final String A = IamModelDemo.BANK;

    @Test
    void aTrustPolicyThatNamesTheAccountNeedsTheCallersPolicyToo() {
        Role r = IamModelDemo.developer();
        Principal withoutPermission = Principal.user(A, "x", Policy.of("n", true, "s3:*", "*"));
        Principal withPermission = Principal.user(A, "y", Policy.of("a", true, "sts:AssumeRole", "*"));
        Map<String, String> mfa = Map.of("aws:MultiFactorAuthPresent", "true");
        assertFalse(r.assume(withoutPermission, mfa, null, "s").decision().allowed());
        assertTrue(r.assume(withPermission, mfa, null, "s").decision().allowed());
        assertFalse(r.assume(withPermission, Map.of(), null, "s").decision().allowed());
    }

    @Test
    void aTrustPolicyThatNamesTheCallerNeedsNothingElseInTheSameAccount() {
        Principal caller = Principal.user(A, "z");
        Policy trust = Policy.parse("t", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"" + caller.arn() + "\"},\"Action\":\"sts:AssumeRole\"}]}");
        Role r = new Role(A, "R", trust, List.of(), null);
        assertTrue(r.assume(caller, Map.of(), null, "s").decision().allowed());
        assertFalse(r.assume(Principal.user(A, "other", Policy.of("a", true, "*", "*")), Map.of(), null, "s").decision().allowed());
    }

    @Test
    void theSessionCarriesTheRolesPoliciesTheBoundaryAndTheSessionPolicy() {
        Role r = IamModelDemo.developer();
        Principal caller = Principal.user(A, "ben", Policy.of("a", true, "sts:AssumeRole", "*"));
        Policy narrow = Policy.of("narrow", true, "s3:GetObject", "*");
        Principal s = r.assume(caller, Map.of("aws:MultiFactorAuthPresent", "true"), narrow, "ben").principal();
        assertEquals("arn:aws:sts::" + A + ":assumed-role/Developer/ben", s.arn());
        assertEquals("Developer", s.roleName());
        assertEquals(narrow, s.sessionPolicy());
    }

    @Test
    void theExternalIdStopsTheConfusedDeputy() {
        IamModelDemo.ReportCo service = new IamModelDemo.ReportCo(Map.of("the-bank", "ext-bank", "mallory", "ext-mallory"));
        Policy trustOpen = Policy.parse("t", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws:iam::" + IamModelDemo.SAAS + ":root\"},\"Action\":\"sts:AssumeRole\"}]}");
        Policy trustGuarded = Policy.parse("t", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws:iam::" + IamModelDemo.SAAS + ":root\"},\"Action\":\"sts:AssumeRole\","
                + "\"Condition\":{\"StringEquals\":{\"sts:ExternalId\":\"ext-bank\"}}}]}");
        Policy reads = Policy.parse("r", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"s3:GetObject\",\"Resource\":\"arn:aws:s3:::bank-reports/*\"}]}");
        Role open = new Role(A, "ReportCoAccess", trustOpen, List.of(reads), null);
        Role guarded = new Role(A, "ReportCoAccess", trustGuarded, List.of(reads), null);
        assertTrue(service.readFor("mallory", open).allowed());
        assertTrue(service.readFor("the-bank", guarded).allowed());
        assertFalse(service.readFor("mallory", guarded).allowed());
    }

    @Test
    void sourceArnConditionsStopTheServiceToServiceVersion() {
        Policy topic = Policy.parse("t", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"" + IamModelDemo.SAAS + "\"},\"Action\":\"sns:Publish\",\"Resource\":\"*\","
                + "\"Condition\":{\"ArnLike\":{\"aws:SourceArn\":\"arn:aws:s3:::bank-*\"}}}]}");
        Principal service = Principal.user(IamModelDemo.SAAS, "n", Policy.of("p", true, "sns:Publish", "*"));
        assertTrue(Evaluator.decide(new Request(service, "sns:Publish", "arn:aws:sns:::t", Map.of("aws:SourceArn", "arn:aws:s3:::bank-statements")), Evaluator.Guard.of(topic, A)).allowed());
        assertFalse(Evaluator.decide(new Request(service, "sns:Publish", "arn:aws:sns:::t", Map.of("aws:SourceArn", "arn:aws:s3:::mallory")), Evaluator.Guard.of(topic, A)).allowed());
    }

    @Test
    void escalationFindsTheRoleThatRewritesItsOwnPolicyAndThePassedAdminRole() {
        Policy trust = Policy.parse("trust", "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws:iam::" + A + ":root\"},\"Action\":\"sts:AssumeRole\"}]}");
        Role dev = new Role(A, "DevRole", trust, List.of(Policy.parse("DevPolicy", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"s3:*\",\"iam:CreatePolicyVersion\"],\"Resource\":\"*\"}]}")), null);
        Role admin = new Role(A, "AdminRole", trust, List.of(Policy.of("admin", true, "*", "*")), null);
        Principal carol = Principal.user(A, "carol", Policy.of("a", true, "sts:AssumeRole", dev.arn()));
        Principal ben = Principal.user(A, "ben", Policy.parse("b", "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"iam:PassRole\",\"lambda:CreateFunction\",\"lambda:InvokeFunction\"],\"Resource\":\"*\"}]}"));
        Principal ana = Principal.user(A, "ana", Policy.of("r", true, "s3:GetObject", "*"));
        Escalation.World w = new Escalation.World(A, List.of(ana, ben, carol), List.of(dev, admin), Map.of("DevPolicy", "DevRole"));
        assertEquals(List.of(), Escalation.paths(ana, w));
        assertEquals(1, Escalation.paths(ben, w).size());
        assertTrue(Escalation.paths(carol, w).get(0).contains("iam:CreatePolicyVersion on DevPolicy"));
        assertTrue(Escalation.isAdmin(Principal.user(A, "root", Policy.of("a", true, "*", "*")), A));
        assertFalse(Escalation.isAdmin(ana, A));
    }
}
