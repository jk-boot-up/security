package com.jk.explore.security.threatmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ModelTest {

    private final Model bank = BankModel.baseline();

    @Test
    void theBankModelIsComplete() {
        assertEquals(7, bank.elements().size());
        assertEquals(8, bank.flows().size());
        assertTrue(bank.problems().isEmpty());
    }

    @Test
    void crossingsAndBoundariesFollowTheZones() {
        Flow f2 = flow("F2");
        assertTrue(bank.crosses(f2));
        assertEquals("internet boundary", bank.boundaryOf(f2).orElseThrow().name());
        assertTrue(bank.raisesTrust(f2));
        assertFalse(bank.crosses(flow("F1")));
        assertFalse(bank.raisesTrust(flow("F6")));
    }

    @Test
    void aFlowThatSkipsABoundaryAndAStoreWithoutAnOwnerAreReported() {
        var problems = BankModel.draft().problems();
        assertEquals(2, problems.size());
        assertTrue(problems.get(0).startsWith("D1 goes from device to internal"));
        assertTrue(problems.get(1).startsWith("S3 card vault has no owner"));
    }

    @Test
    void unknownAndUnconnectedElementsAreReported() {
        Model lonely = bank.with(new Element("P9", "batch job", Kind.PROCESS, BankModel.INTERNAL, "ops", false, Set.of()));
        assertTrue(lonely.problems().contains("P9 batch job is not connected to anything"));
        Model dangling = bank.with(new Flow("X1", "P3", "Q7", "report", true, true, false));
        assertTrue(dangling.problems().contains("X1 names an element that is not in the model"));
        assertThrows(IllegalArgumentException.class, () -> bank.element("Q7"));
    }

    private Flow flow(String id) {
        return bank.flows().stream().filter(f -> f.id().equals(id)).findFirst().orElseThrow();
    }
}
