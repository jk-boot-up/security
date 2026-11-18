package com.jk.explore.security.pytmmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The teaching class Condition, by hand and against the recorded pytm findings. Needs no Python. */
class ConditionTest {

    @Test
    void readsOrAndNotAndBracketsInPythonsOrder() {
        String c = "target.controls.a is False or target.controls.b is False and target.c is True";
        assertTrue(Condition.holds(c, Set.of()));               // True or (True and False): and before or, as in Python
        assertTrue(Condition.holds(c, Set.of("a", "c")));       // False or (True and True)
        assertFalse(Condition.holds(c, Set.of("a", "b", "c"))); // False or (False and True)
        assertTrue(Condition.holds("(target.a is True or target.b is True) and not target.c is True", Set.of("b")));
        assertFalse(Condition.holds("(target.a is True or target.b is True) and not target.c is True", Set.of("b", "c")));
        assertFalse(Condition.simple("not target.controls.isEncrypted or target.tlsVersion < target.sink.minTLSVersion"));
        assertEquals(List.of(new Condition.Term("isHardened", false), new Condition.Term("hasAccessControl", false)),
                Condition.terms("target.controls.isHardened is False or target.controls.hasAccessControl is False"));
    }

    @Test
    void givesPytmsExactFindingsForEverySimpleThreatElementAndStage() throws Exception {
        Recorded pytm = new Recorded(Recorded.DIR);
        List<ThreatTool.Threat> simple = pytm.library().stream().filter(t -> Condition.simple(t.condition())).toList();
        assertEquals(91, simple.size());
        int checks = 0;
        for (Answers.Stage stage : Answers.Stage.values()) {
            String python = PytmDemo.model(Design.bank(), stage);
            List<ThreatTool.Finding> found = pytm.findings("bank-" + stage, python);
            for (Design.Element e : Design.bank().elements()) {
                for (ThreatTool.Threat t : simple) {
                    if (t.targets().contains(e.kind().pytm)) {
                        boolean byPytm = found.stream().anyMatch(f -> f.threat().equals(t.id()) && f.target().equals(e.name()));
                        assertEquals(byPytm, Condition.holds(t.condition(), Writer.trueAnswers(python, e.id())), stage + " " + t.id() + " " + e.name());
                        checks++;
                    }
                }
            }
        }
        assertEquals(730, checks);
    }
}
