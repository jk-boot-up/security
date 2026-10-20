package com.jk.explore.security.ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class IncidentTest {

    @Test
    void preparationIsScoredAndItsGapsAreNamed() {
        assertEquals(0, Preparation.none().score());
        assertEquals(1, Preparation.monitoringOnly().score());
        assertEquals(6, Preparation.full().score());
        assertEquals(List.of(), Preparation.full().missing());
        assertTrue(Preparation.none().missing().contains("backups never tested"));
        assertTrue(new Preparation(true, true, true, 3, true, true, true).missing().contains("logs not kept centrally for at least 7 days"));
    }

    @Test
    void withoutMonitoringTheTheftIsFoundByACustomerThreeDaysLater() {
        assertEquals(3 * 24 * 60, new World(Preparation.none()).detectedAt);
        assertEquals(30, new World(Preparation.monitoringOnly()).detectedAt);
    }

    @Test
    void theAttackerCopiesAThousandRowsAMinuteFromMinuteTwentyUntilStopped() {
        World w = new World(Preparation.full());
        w.advance(19);
        assertEquals(0, w.exfiltrated);
        w.advance(11);
        assertEquals(10_000, w.exfiltrated);
        w.advance(1000);
        assertEquals(World.CUSTOMERS, w.exfiltrated, "everything is copied by minute 70");
    }

    @Test
    void isolatingWeb01StopsTheCopyingAndRemembersHowMuchHadGone() {
        World w = new World(Preparation.full());
        new Responder(w).detect().isolateWeb01();
        assertFalse(w.exfilActive());
        assertEquals(15_000, w.rowsWhenStopped);
        w.advance(100);
        assertEquals(15_000, w.exfiltrated);
    }

    @Test
    void killingTheSessionAlsoStopsIt() {
        World w = new World(Preparation.full());
        new Responder(w).detect().killDatabaseSessions();
        assertEquals(12_000, w.rowsWhenStopped);
    }

    @Test
    void rebootingDestroysMemoryEvidenceAndKeepsThePersistence() {
        World w = new World(Preparation.full());
        new Responder(w).detect().isolateWeb01().captureEvidence();
        assertTrue(w.memoryCaptured);
        new Responder(w).rebootWeb01();
        assertFalse(w.memoryCaptured);
        assertTrue(w.footholdsLeft().contains("cron job on web01"));
        assertTrue(w.footholdsLeft().contains("SSH key on web01"));
    }

    @Test
    void rebuildingFromAnImageRemovesWhatLivesOnTheHostButNotTheRest() {
        World w = new World(Preparation.full());
        new Responder(w).detect().rebuildWeb01();
        List<String> left = w.footholdsLeft();
        assertFalse(left.contains("cron job on web01") || left.contains("SSH key on web01"));
        assertTrue(left.contains("hidden admin account svc-backup") && left.contains("unpatched flaw in the application") && left.contains("stolen database password"));
        assertEquals(40, w.minute - 30);
    }

    @Test
    void rebuildingByHandIsSlower() {
        World w = new World(Preparation.monitoringOnly());
        new Responder(w).detect().rebuildWeb01();
        assertEquals(120, w.minute - 30);
    }

    @Test
    void youCannotDisableAnAccountYouDoNotKnowAbout() {
        World blind = new World(Preparation.monitoringOnly());
        new Responder(blind).detect().scopeFromLogs().disableBackdoorAccount();
        assertFalse(blind.scoped);
        assertTrue(blind.adminBackdoor);
        World sighted = new World(Preparation.full());
        new Responder(sighted).detect().scopeFromLogs().disableBackdoorAccount();
        assertTrue(sighted.scoped);
        assertFalse(sighted.adminBackdoor);
    }

    @Test
    void scopingNeedsLogsThatReachBackToTheStart() {
        World late = new World(new Preparation(true, false, true, 2, true, true, true));
        new Responder(late).detect().scopeFromLogs();
        assertFalse(late.scoped, "two days of logs do not reach back three days");
        World ok = new World(new Preparation(true, false, true, 7, true, true, true));
        new Responder(ok).detect().scopeFromLogs();
        assertTrue(ok.scoped);
    }

    @Test
    void thePanickedResponseFailsInEveryWay() {
        Outcome o = Outcome.of("A", Playbooks.panic());
        assertEquals(3 * 24 * 60, o.foundAfterMinutes());
        assertEquals(-1, o.rowsCopiedBeforeStop());
        assertEquals(World.CUSTOMERS, o.rowsCopiedInTotal());
        assertEquals(4, o.footholdsLeft().size());
        assertFalse(o.evidenceKept() || o.rootCauseKnown() || o.recovered());
        assertTrue(o.attackerReturned());
    }

    @Test
    void theImprovisedResponseStopsTheCopyingButNotTheAttacker() {
        Outcome o = Outcome.of("B", Playbooks.improvised());
        assertEquals(15_000, o.rowsCopiedBeforeStop());
        assertEquals(3, o.footholdsLeft().size());
        assertTrue(o.attackerReturned());
        assertFalse(o.recovered());
        assertEquals(130, o.outageMinutes());
    }

    @Test
    void thePlaybookKeepsEvidenceKnowsTheRootCauseAndRecovers() {
        Outcome o = Outcome.of("C", Playbooks.playbook());
        assertEquals(15_000, o.rowsCopiedBeforeStop());
        assertEquals(List.of(), o.footholdsLeft());
        assertTrue(o.evidenceKept() && o.rootCauseKnown() && o.recovered());
        assertFalse(o.attackerReturned());
    }

    @Test
    void theClockPrintsDaysAndTimes() {
        World w = new World(Preparation.full());
        assertEquals("day 0 01:00", w.clock());
        w.advance(25 * 60);
        assertEquals("day 1 02:00", w.clock());
    }
}
