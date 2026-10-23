package com.jk.explore.security.detectioncode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SigmaTest {

    private static Rule rule(String detection) {
        return new Rule(Sigma.documents("name: t\ntags: [attack.t1059.004]\ndetection:\n" + detection).getFirst());
    }

    private static Map<String, String> ev(String... kv) {
        return Corpus.m(kv);
    }

    @Test
    void contains_startswith_endswith_and_equals() {
        Rule r = rule("  a:\n    X|contains: mid\n  b:\n    Y|startswith: begin\n  c:\n    Z|endswith: end\n  d:\n    W: exact\n  condition: a and b and c and d\n");
        assertTrue(r.matches(ev("X", "a mid b", "Y", "beginning", "Z", "the end", "W", "exact")));
        assertFalse(r.matches(ev("X", "a mid b", "Y", "not beginning", "Z", "the end", "W", "exact")));
        assertFalse(r.matches(ev("X", "a mid b", "Y", "beginning", "Z", "the end", "W", "exact more")));
    }

    @Test
    void valuesAreComparedWithNoRegardToCase() {
        Rule r = rule("  s:\n    X|contains: UNION SELECT\n  condition: s\n");
        assertTrue(r.matches(ev("X", "1 union Select 2")));
    }

    @Test
    void aMissingFieldMatchesNothingSoNotOfItIsTrue() {
        Rule r = rule("  s:\n    X: a\n  f:\n    Y: b\n  condition: s and not f\n");
        assertTrue(r.matches(ev("X", "a")));
        assertFalse(r.matches(ev("X", "a", "Y", "b")));
        assertFalse(r.matches(ev("Y", "c")));
    }

    @Test
    void aListIsOrAndTheAllModifierIsAnd() {
        Rule any = rule("  s:\n    X|contains:\n      - one\n      - two\n  condition: s\n");
        Rule all = rule("  s:\n    X|contains|all:\n      - one\n      - two\n  condition: s\n");
        assertTrue(any.matches(ev("X", "only one")));
        assertFalse(all.matches(ev("X", "only one")));
        assertTrue(all.matches(ev("X", "one and two")));
    }

    @Test
    void wildcardsInsideAValue() {
        Rule r = rule("  s:\n    X: 'a*c?'\n  condition: s\n");
        assertTrue(r.matches(ev("X", "abbbcd")));
        assertFalse(r.matches(ev("X", "abbbc")));
    }

    @Test
    void oneOfAndAllOfSelectionGroups() {
        String sels = "  sel_a:\n    X: a\n  sel_b:\n    Y: b\n  other:\n    Z: z\n";
        assertTrue(rule(sels + "  condition: 1 of sel_*\n").matches(ev("X", "a")));
        assertFalse(rule(sels + "  condition: all of sel_*\n").matches(ev("X", "a")));
        assertTrue(rule(sels + "  condition: all of sel_*\n").matches(ev("X", "a", "Y", "b")));
        assertTrue(rule(sels + "  condition: 1 of them\n").matches(ev("Z", "z")));
    }

    @Test
    void parenthesesChangeTheMeaning() {
        String sels = "  a:\n    X: 1\n  b:\n    Y: 1\n  c:\n    Z: 1\n";
        assertTrue(rule(sels + "  condition: a or b and c\n").matches(ev("X", "1")));
        assertFalse(rule(sels + "  condition: (a or b) and c\n").matches(ev("X", "1")));
    }

    @Test
    void whatIsNotImplementedIsRefusedNotIgnored() {
        assertThrows(IllegalArgumentException.class, () -> rule("  s:\n    X|re: 'a.*'\n  condition: s\n"));
        assertThrows(IllegalArgumentException.class, () -> rule("  s:\n    - keyword\n  condition: s\n"));
        assertThrows(IllegalArgumentException.class, () -> rule("  s:\n    X: a\n  condition: missing\n"));
    }

    @Test
    void techniquesComeFromTheTags() {
        assertTrue(rule("  s:\n    X: a\n  condition: s\n").techniques().contains("T1059.004"));
    }
}
