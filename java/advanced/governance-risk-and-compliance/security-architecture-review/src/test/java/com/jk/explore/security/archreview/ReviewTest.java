package com.jk.explore.security.archreview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReviewTest {

    private final Design v1 = load("design/v1/bank.dsl");
    private final Map<String, Adr> adrs1 = adrs("design/v1/adr");

    private static Design load(String p) {
        try {
            return Design.load(Path.of(p));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Adr> adrs(String p) {
        try {
            return Adr.load(Path.of(p));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void theFirstDesignHasNinePartsNineFlowsAndSixZones() {
        assertEquals(9, v1.parts().size());
        assertEquals(9, v1.flows().size());
        assertEquals(6, v1.parts().stream().map(Design.Part::zone).distinct().count());
    }

    @Test
    void kindsAreTakenFromTheModel() {
        assertEquals(Design.Kind.DATASTORE, v1.parts().stream().filter(p -> p.id().equals("ledgerdb")).findFirst().orElseThrow().kind());
        assertEquals(Design.Kind.EXTERNAL, v1.parts().stream().filter(p -> p.id().equals("cards")).findFirst().orElseThrow().kind());
        assertEquals(Design.Kind.PERSON, v1.parts().stream().filter(p -> p.id().equals("customer")).findFirst().orElseThrow().kind());
    }

    @Test
    void strideQuestionsFollowTheKindOfEachPartAndTheFlows() {
        assertEquals(3 * 2 + 4 * 6 + 2 * 4 + 3 * 9, Stride.questions(v1));
    }

    @Test
    void theDirectPathRuleReportsOnlyTheConfidentialStore() {
        List<Finding> f = Rules.run(v1, adrs1, List.of("no-direct-path-to-data"));
        assertEquals(List.of("web -> ledgerdb"), f.stream().map(Finding::subject).toList());
    }

    @Test
    void aSupersededAndAProposedDecisionAreNotAccepted() {
        List<Finding> f = Rules.run(v1, adrs1, List.of("decision-is-accepted"));
        assertEquals(List.of("cards", "sessions"), f.stream().map(Finding::subject).toList());
    }

    @Test
    void aReferenceToADecisionThatDoesNotExistIsReported() {
        List<Finding> f = Rules.run(v1, Map.of(), List.of("decision-is-accepted"));
        assertTrue(f.stream().allMatch(x -> x.message().contains("does not exist")));
        assertFalse(f.isEmpty());
    }

    @Test
    void theEveryFlowRuleFindsTheInternalCallTheCrossZoneRulesMiss() {
        assertTrue(Rules.run(v1, adrs1, Rules.FIRST_SET).stream().noneMatch(f -> f.subject().equals("api -> ledger")));
        assertTrue(Rules.run(v1, adrs1, Rules.ALL).stream().anyMatch(f -> f.subject().equals("api -> ledger")));
    }

    @Test
    void theReviewedDesignHasNoFindings() throws Exception {
        assertEquals(List.of(), Rules.run(load("design/v2/bank.dsl"), Adr.load(Path.of("design/v2/adr")), Rules.ALL));
    }

    @Test
    void decisionsAreReadWithTheirStatus() {
        assertEquals(4, adrs1.size());
        assertEquals("Superseded by 5", adrs1.get("0003").status());
        assertFalse(adrs1.get("0004").accepted());
        assertTrue(adrs1.get("0001").accepted());
    }

    @Test
    void aPlantedWeaknessWithNoRuleIsNeverReported() {
        Planted hidden = Planted.FIRST_DESIGN.get(Planted.FIRST_DESIGN.size() - 1);
        assertEquals(null, hidden.rule());
        assertFalse(hidden.reportedBy(Rules.run(v1, adrs1, Rules.ALL)));
    }

    @Test
    void theFlowCheckFiresExactlyTheFlowRulesOfRulesForEveryFlowOfBothDesigns() throws Exception {
        List<String> flowRules = List.of("no-direct-path-to-data", "cross-zone-encrypted", "cross-zone-authenticated", "every-flow-encrypted-and-authenticated");
        for (String v : List.of("v1", "v2")) {
            Design d = load("design/" + v + "/bank.dsl");
            List<Finding> findings = Rules.run(d, Map.of(), flowRules);
            for (Design.Flow f : d.flows()) {
                List<String> byRules = findings.stream().filter(x -> x.subject().equals(f.id())).map(Finding::rule).sorted().toList();
                assertEquals(byRules, new FlowCheck(f).rulesThatFire().stream().sorted().toList(), v + " " + f.id());
            }
        }
    }

    @Test
    void strideAsksTwoSixFourAndThreeLetters() {
        assertEquals(List.of("S", "R"), Stride.letters(Design.Kind.EXTERNAL));
        assertEquals(6, Stride.letters(Design.Kind.PROCESS).size());
        assertEquals(List.of("T", "R", "I", "D"), Stride.letters(Design.Kind.DATASTORE));
        assertEquals(62, Stride.questions(load("design/v2/bank.dsl")));
    }

    @Test
    void theChangesBetweenTheDesignsAreListed() throws Exception {
        Design v2 = load("design/v2/bank.dsl");
        assertEquals(List.of(), Changes.parts(v1, v1));
        assertEquals(4, Changes.parts(v1, v2).size());
        assertTrue(Changes.flows(v1, v2).contains(new Changes.Change("web -> ledgerdb", "flow", "drawn", "removed")));
        assertEquals(6, Changes.flows(v1, v2).size());
        assertEquals(List.of(new Changes.Change("0003", "status", "Superseded by 5", "removed"), new Changes.Change("0004", "status", "Proposed", "Accepted"),
                new Changes.Change("0005", "status", "-", "Accepted")), Changes.decisions(adrs1, adrs("design/v2/adr")));
    }

    @Test
    void partsKeepTheOrderOfTheDslFile() {
        assertEquals(List.of("customer", "staff", "cards", "web", "api", "ledger", "ledgerdb", "sessions", "reports"), v1.parts().stream().map(Design.Part::id).toList());
    }
}
