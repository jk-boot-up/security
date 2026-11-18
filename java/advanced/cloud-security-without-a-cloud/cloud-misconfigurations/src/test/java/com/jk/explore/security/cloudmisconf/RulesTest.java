package com.jk.explore.security.cloudmisconf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The nine rules on plain values: no emulator, no SDK. */
class RulesTest {

    static final Snapshot.Bucket SAFE = new Snapshot.Bucket("bank-audit", null, null, true, "AES256", true);

    static Snapshot only(Snapshot.Bucket b) {
        return new Snapshot(List.of(b), List.of(), List.of(), List.of(), 0, 0);
    }

    static List<String> rules(Snapshot s) {
        return Rules.check(s).stream().map(Finding::rule).toList();
    }

    @Test
    void aSafeBucketHasNoFindings() {
        assertTrue(Rules.check(only(SAFE)).isEmpty());
    }

    @Test
    void anOpenBucketBreaksEveryBucketRuleWorstFirst() {
        Snapshot.Bucket open = new Snapshot.Bucket("bank-statements", "READ", "s3:GetObject", null, null, false);
        assertEquals(List.of("s3-public-acl", "s3-public-policy", "s3-no-public-access-block", "s3-no-encryption", "s3-no-versioning"), rules(only(open)));
    }

    @Test
    void aPartialBlockIsNotEnough() {
        Snapshot.Bucket partial = new Snapshot.Bucket("bank-exports", null, null, false, "AES256", true);
        assertEquals(List.of("s3-no-public-access-block"), rules(only(partial)));
    }

    @Test
    void onlyStarOnStarIsAWildcardAdmin() {
        Snapshot s = new Snapshot(List.of(), List.of(
                new Snapshot.Policy("developer-everything", List.of(new Snapshot.Statement("Allow", null, "*", "*"))),
                new Snapshot.Policy("all-of-s3", List.of(new Snapshot.Statement("Allow", null, "*", "arn:aws:s3:::bank-statements/*"))),
                new Snapshot.Policy("deny-all", List.of(new Snapshot.Statement("Deny", null, "*", "*")))), List.of(), List.of(), 0, 0);
        assertEquals(List.of("iam-wildcard-admin"), rules(s));
    }

    @Test
    void adminPortsToTheWorldAreFoundButHttpsIsNot() {
        Snapshot s = new Snapshot(List.of(), List.of(), List.of(
                new Snapshot.Group("web", List.of(new Snapshot.Ingress(443, "0.0.0.0/0"), new Snapshot.Ingress(22, "0.0.0.0/0"))),
                new Snapshot.Group("database", List.of(new Snapshot.Ingress(5432, "10.0.1.0/24")))), List.of(), 0, 0);
        List<Finding> f = Rules.check(s);
        assertEquals(1, f.size());
        assertEquals("port 22 is open to 0.0.0.0/0", f.getFirst().why());
    }

    @Test
    void tokensOptionalIsImdsVersionOne() {
        Snapshot s = new Snapshot(List.of(), List.of(), List.of(),
                List.of(new Snapshot.Server("web", "optional", 2), new Snapshot.Server("api", "required", 1)), 0, 0);
        assertEquals(List.of("ec2-imdsv1"), rules(s));
    }

    @Test
    void unencryptedDisksAreCountedInOneFinding() {
        assertEquals("2 disks", Rules.check(new Snapshot(List.of(), List.of(), List.of(), List.of(), 3, 1)).getFirst().resource());
        assertTrue(Rules.check(new Snapshot(List.of(), List.of(), List.of(), List.of(), 2, 2)).isEmpty());
    }

    @Test
    void aPolicyDocumentIsReadWithItsPrincipal() {
        List<Snapshot.Statement> st = Scanner.statements(Environment.PUBLIC_BUCKET_POLICY);
        assertEquals(new Snapshot.Statement("Allow", "*", "s3:GetObject", "arn:aws:s3:::bank-exports/*"), st.getFirst());
        assertEquals("s3:GetObject", Scanner.statements(Environment.POLICY_READ).getFirst().action());
    }

    @Test
    void theSnapshotLinesStayShort() {
        Snapshot s = new Snapshot(List.of(SAFE), List.of(new Snapshot.Policy("statements-reader",
                Scanner.statements(Environment.POLICY_READ))), List.of(), List.of(), 2, 0);
        assertEquals("statements-reader     Allow s3:GetObject on bank-statements/*", s.lines().get(2));
        assertEquals("disks                 2, 0 encrypted", s.lines().get(3));
    }

    @Test
    void theSeveritiesAddUp() {
        Snapshot.Bucket open = new Snapshot.Bucket("bank-statements", "READ", null, null, null, false);
        assertEquals("1 CRITICAL + 1 HIGH + 1 MEDIUM + 1 LOW = 4", CloudMisconfigurationsDemo.bySeverity(Rules.check(only(open))));
    }
}
