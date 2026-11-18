package com.jk.explore.security.policyascode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PolicyTest {

    static final Path POLICY = Path.of("policy");

    @Test
    void rulesAreReadFromTheMessages() {
        assertEquals(Set.of("R2", "R8"), Cases.rules(List.of("R2 image x", "R8 image x", "R2 image y")));
    }

    @Test
    void theCasesCoverEveryRuleOnce() {
        Set<String> expected = new java.util.TreeSet<>();
        Cases.all().forEach(c -> expected.addAll(c.expected()));
        assertEquals(10, expected.size());
        assertTrue(Cases.all().get(0).expected().isEmpty());
    }

    @Test
    void theGoodObjectHasAllTheFieldsThePolicyAsksFor() {
        assertEquals("registry.bank.example/payments:2.3.1", Cases.container(Cases.good()).get("image").asString());
    }

    @Test
    void mutantsSwitchOffExactlyOneRule() throws Exception {
        String source = Files.readString(POLICY.resolve("main.rego"));
        int n = Mutants.count(source);
        assertEquals(12, n);
        assertEquals(n - 1, Mutants.count(Mutants.disable(source, 3)));
        assertThrows(IllegalArgumentException.class, () -> Mutants.disable(source, n));
    }

    @Test
    void eachRuleIsLabelledByTheCommentAboveIt() throws Exception {
        String source = Files.readString(POLICY.resolve("main.rego"));
        assertEquals("R1: an allowed kind", Mutants.label(source, 0));
        assertEquals("R5: a cpu limit", Mutants.label(source, 4));
        assertEquals("R10: at least one container", Mutants.label(source, Mutants.count(source) - 1));
        for (int i = 0; i < Mutants.count(source); i++) {
            assertTrue(Mutants.label(source, i).matches("R\\d+: .+"), Mutants.label(source, i));
        }
    }

    @Test
    void aStepShowsUndefinedWhenOpaLeavesTheRuleOut() {
        var json = tools.jackson.databind.json.JsonMapper.builder().build();
        assertEquals("undefined", Steps.show(null));
        assertEquals("true", Steps.show(json.readTree("true")));
        assertEquals("0", Steps.show(json.readTree("0")));
        assertEquals("Deployment", Steps.show(json.readTree("\"Deployment\"")));
    }

    @Test
    void everyStepNamesARuleOfTheStepsModule() {
        for (var list : List.of(Steps.EMPTY_OBJECT, Steps.NO_CONTAINERS, Steps.NO_SECURITY_CONTEXT)) {
            list.forEach(s -> assertTrue(Steps.MODULE.contains("\n" + s.rule() + " := "), s.rule()));
        }
    }

    @Test
    void theDemoWordsCountsAndDecisions() {
        assertEquals("1 warning", PolicyAsCodeDemo.count(1, "warning"));
        assertEquals("0 failures", PolicyAsCodeDemo.count(0, "failure"));
        assertEquals("allow", PolicyAsCodeDemo.describe(List.of()));
        assertEquals("deny R2 R8", PolicyAsCodeDemo.describe(List.of("R8 b", "R2 a")));
    }

    @Test
    void anAdmissionResponseCarriesTheReasons() {
        assertTrue(Admission.respond(List.of()).allowed());
        Admission.Response r = Admission.respond(List.of("R2 a", "R8 b"));
        assertFalse(r.allowed());
        assertEquals("R2 a; R8 b", r.message());
    }

    @Test
    void aReviewWrapsTheObject() {
        var review = Admission.review(Cases.good());
        assertEquals("AdmissionReview", review.get("kind").asString());
        assertEquals("Deployment", review.get("request").get("object").get("kind").asString());
    }

    @Tag("integration")
    @Test
    void thePolicyDecidesEveryCaseAsTheReviewerExpects() throws Exception {
        for (Cases.Case c : Cases.all()) {
            assertEquals(c.expected(), Cases.rules(Opa.deny(POLICY, "main", c.object())), c.name());
        }
    }

    @Tag("integration")
    @Test
    void theRegoTestsPass() throws Exception {
        Opa.Tests t = Opa.test(null, List.of(POLICY));
        assertEquals(t.total(), t.passed());
        assertTrue(t.total() >= 17);
    }

    @Tag("integration")
    @Test
    void aMissingFieldIsDeniedByTheLockedPolicyAndAllowedByTheNaiveOne() throws Exception {
        var o = Cases.good();
        Cases.container(o).remove("securityContext");
        assertTrue(Opa.deny(POLICY, "naive", o).isEmpty());
        assertFalse(Opa.deny(POLICY, "main", o).isEmpty());
    }

    @Tag("integration")
    @Test
    void theFullSuiteKillsEveryMutant() throws Exception {
        Mutants.Score s = Mutants.score(POLICY, null, Path.of("build/mutants-test"));
        assertEquals(12, s.killed());
        assertEquals(17, s.tests());
    }

    @Tag("integration")
    @Test
    void theToolsAreThePinnedVersions() throws Exception {
        Opa.requirePinnedVersions();
    }

    @Tag("integration")
    @Test
    void theStepsAgreeWithThePolicy() throws Exception {
        var empty = Steps.values(POLICY, Steps.EMPTY_OBJECT, tools.jackson.databind.json.JsonMapper.builder().build().createObjectNode(),
                Path.of("build/steps-test"));
        assertEquals(List.of("undefined", "undefined", "true"), List.copyOf(empty.values()));
        var good = Steps.values(POLICY, Steps.NO_SECURITY_CONTEXT, Cases.good(), Path.of("build/steps-test"));
        assertEquals(List.of("true", "false", "undefined"), List.copyOf(good.values()));
    }
}
