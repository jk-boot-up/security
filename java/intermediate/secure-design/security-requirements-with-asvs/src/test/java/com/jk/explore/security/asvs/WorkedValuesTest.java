package com.jk.explore.security.asvs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The values the demo works out step by step: what each level adds, and how the password check reads a stored value. */
class WorkedValuesTest {

    private static List<String> idsAt(Level level) {
        return Catalogue.all().stream().filter(r -> r.level() == level).map(Requirement::id).toList();
    }

    @Test
    void eachLevelAddsItsOwnRequirementsToTheOnesBelow() {
        assertEquals(8, idsAt(Level.L1).size());
        assertEquals(List.of("SR-09", "SR-10", "SR-12", "SR-14"), idsAt(Level.L2));
        assertEquals(List.of("SR-11", "SR-13"), idsAt(Level.L3));
    }

    @Test
    void aPlainSha256HashIsTheSameOnEveryStoreAndFailsTheCheck() {
        String a = new SampleApp(SampleApp.Settings.store().withFastPasswordHash()).storedPasswordFor(Checks.SAMPLE_PASSWORD);
        String b = new SampleApp(SampleApp.Settings.store().withFastPasswordHash()).storedPasswordFor(Checks.SAMPLE_PASSWORD);
        assertEquals(a, b);
        assertEquals("sha256", a.split("\\$")[0]);
        assertEquals(64, a.split("\\$")[1].length());
        assertFalse(Checks.named("password-storage").passes().test(new SampleApp(SampleApp.Settings.store().withFastPasswordHash())));
    }

    @Test
    void aSlowHashIsSaltedSoTwoStoresKeepDifferentValues() {
        String a = new SampleApp(SampleApp.Settings.store()).storedPasswordFor(Checks.SAMPLE_PASSWORD);
        String b = new SampleApp(SampleApp.Settings.store()).storedPasswordFor(Checks.SAMPLE_PASSWORD);
        assertFalse(a.equals(b));
        assertEquals(4, a.split("\\$").length);
    }

    @Test
    void theBankLevelLeavesFourRequirementsToPeople() {
        Assessment l3 = Assessment.of(Catalogue.all(), Level.L3, new SampleApp(SampleApp.Settings.store().withStaffMfa(true)));
        assertEquals(4, l3.totals().get(Assessment.Status.MANUAL));
        assertEquals(10, l3.totals().get(Assessment.Status.MET));
        assertEquals(0, l3.totals().get(Assessment.Status.FAILED));
    }
}
