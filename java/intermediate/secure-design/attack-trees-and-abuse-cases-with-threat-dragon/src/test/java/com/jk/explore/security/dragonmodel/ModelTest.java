package com.jk.explore.security.dragonmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The model file, the checks over it and the tree it comes from, with no Docker. */
class ModelTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void theTreeHasEightAbuseCasesAndThePlanHasFourSteps() {
        assertEquals(8, Cases.all(BankTree.baseline()).size());
        assertEquals(4, Planner.plan(BankTree.baseline(), Defence.all(), 5_000).size());
        assertEquals(50.0, Analysis.of(BankTree.baseline()).minCost());
    }

    @Test
    void severityComesFromCostAndChance() {
        List<Cases.Entry> cases = Cases.all(BankTree.baseline());
        assertEquals("High", cases.stream().filter(c -> c.id().equals("AC5")).findFirst().orElseThrow().severity());
        assertEquals("Medium", cases.stream().filter(c -> c.id().equals("AC4")).findFirst().orElseThrow().severity());
        assertEquals("Low", cases.stream().filter(c -> c.id().equals("AC7")).findFirst().orElseThrow().severity());
    }

    @Test
    void theModelHasTheFieldsThatTheEditorReads() {
        JsonNode m = JSON.readTree(ModelBuilder.json(BankConfig.weak()));
        assertEquals("2.6.2", m.get("version").asString());
        assertTrue(m.get("summary").has("title"));
        JsonNode diagram = m.get("detail").get("diagrams").get(0);
        assertEquals("STRIDE", diagram.get("diagramType").asString());
        assertEquals(13, diagram.get("cells").size());
        Set<String> shapes = new java.util.TreeSet<>();
        diagram.get("cells").forEach(c -> shapes.add(c.get("shape").asString()));
        assertEquals(Set.of("actor", "process", "store", "flow", "trust-boundary-box"), shapes);
        assertEquals(8, m.get("detail").get("threatTop").asInt());
    }

    @Test
    void identifiersAreStableSoThatTheFileDiffsCleanly() {
        assertEquals(ModelBuilder.json(BankConfig.weak()), ModelBuilder.json(BankConfig.weak()));
    }

    @Test
    void everyThreatIsOpenUntilADefenceAnswersIt() {
        String weak = ModelBuilder.json(BankConfig.weak());
        assertEquals(8, ModelChecks.count(weak, "Open"));
        String owned = ModelBuilder.json(BankConfig.weak().with("ownership"));
        assertEquals(7, ModelChecks.count(owned, "Open"));
        assertTrue(ModelChecks.statuses(owned).contains("AC5 Mitigated"));
        assertTrue(JSON.readTree(owned).toString().contains("In place: OWN"));
    }

    @Test
    void theFilesOfThePlanHaveNoProblems() {
        BankConfig c = BankConfig.weak();
        assertEquals(List.of(), ModelChecks.check(ModelBuilder.json(c)));
        for (Planner.Step s : Planner.plan(BankTree.baseline(), Defence.all(), 5_000)) {
            c = c.with(s.defence().configChange());
            assertEquals(List.of(), ModelChecks.check(ModelBuilder.json(c)));
        }
        assertEquals(2, ModelChecks.count(ModelBuilder.json(c), "Open"));
    }

    @Test
    void eachFaultIsFoundByItsOwnRule() {
        String end = ModelBuilder.json(BankConfig.weak().with("ownership").with("csrf").with("passkey").with("csp"));
        assertEquals(List.of("mitigated-names-its-defence"), rules(mutated(end, "no mitigation")));
        assertEquals(List.of("open-flag-matches-threats"), rules(mutated(end, "stale flag")));
        assertEquals(List.of("flow-ends-exist"), rules(mutated(end, "lost end")));
        assertEquals(List.of("threat-top-is-highest-number"), rules(mutated(end, "bad count")));
    }

    private static String mutated(String model, String how) {
        try {
            var m = ThreatDragonDemo.class.getDeclaredMethod("mutate", String.class, String.class);
            m.setAccessible(true);
            return (String) m.invoke(null, model, how);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> rules(String model) {
        return ModelChecks.check(model).stream().map(ModelChecks.Problem::rule).distinct().collect(Collectors.toList());
    }

    @Test
    void aThreatWithoutATypeFromTheSixIsRefused() {
        String bad = ModelBuilder.json(BankConfig.weak()).replace("\"type\" : \"Spoofing\"", "\"type\" : \"Swindling\"");
        assertFalse(ModelChecks.check(bad).isEmpty());
        assertEquals("stride-type", ModelChecks.check(bad).get(0).rule());
    }
}
