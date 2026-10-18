package com.jk.explore.security.asvs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChecksAndAssessmentTest {

    private static final SampleApp STORE = new SampleApp(SampleApp.Settings.store());

    private static boolean check(String name, SampleApp app) {
        return Checks.named(name).passes().test(app);
    }

    @Test
    void theStorePassesEveryCheckButStaffMfa() {
        for (String name : Checks.all().keySet()) {
            assertEquals(!name.equals("staff-mfa"), check(name, STORE), name);
        }
    }

    @Test
    void aDebugBuildLeaksItsStackTraceAndOpensTheDebugEndpoint() {
        SampleApp debug = new SampleApp(SampleApp.Settings.store().withDebug(true));
        assertFalse(check("error-leak", debug));
        assertFalse(check("debug-off", debug));
        assertTrue(debug.errorPage(new IllegalStateException("x")).contains("\tat "));
    }

    @Test
    void aFastPasswordHashFailsTheStorageCheck() {
        assertFalse(check("password-storage", new SampleApp(SampleApp.Settings.store().withFastPasswordHash())));
    }

    @Test
    void theStoredPasswordIsSaltedAndUsesTheConfiguredRounds() {
        String stored = STORE.storedPasswordFor("pw".toCharArray());
        String[] parts = stored.split("\\$");
        assertEquals("pbkdf2-sha256", parts[0]);
        assertEquals("600000", parts[1]);
        assertFalse(stored.contains("pw"));
    }

    @Test
    void anErrorPageWithoutDebugShowsOnlyAReference() {
        String page = new SampleApp(SampleApp.Settings.store()).errorPage(new IllegalStateException("order database unavailable"));
        assertEquals("<h1>Something went wrong</h1><p>Reference E-1</p>", page);
    }

    @Test
    void sessionIdsAreRandomAndRotate() {
        String a = SampleApp.newSessionId();
        assertEquals(22, a.length());
        assertFalse(a.equals(SampleApp.newSessionId()));
        assertFalse(STORE.signIn(a).equals(a));
    }

    @Test
    void assessmentCountsAtEachLevel() {
        Assessment l2 = Assessment.of(Catalogue.all(), Level.L2, STORE);
        assertEquals(8, l2.totals().get(Assessment.Status.MET));
        assertEquals(List.of("SR-09"), l2.failing());
        assertEquals(3, l2.totals().get(Assessment.Status.MANUAL));
        assertEquals(2, l2.totals().get(Assessment.Status.NOT_APPLICABLE));
        Assessment l1 = Assessment.of(Catalogue.all(), Level.L1, STORE);
        assertEquals(List.of(), l1.failing());
        assertEquals(6, l1.totals().get(Assessment.Status.NOT_APPLICABLE));
    }

    @Test
    void aRegressionIsCaughtAsNewlyFailing() {
        Assessment today = Assessment.of(Catalogue.all(), Level.L2, STORE);
        Assessment regressed = Assessment.of(Catalogue.all(), Level.L2, new SampleApp(SampleApp.Settings.store().withDebug(true)));
        assertEquals(List.of("SR-05", "SR-06"), regressed.newlyFailingSince(today));
        assertEquals(List.of(), today.newlyFailingSince(today));
    }

    @Test
    void notApplicableRequirementsAreLeftOutOfTheChapterReport() {
        Assessment l2 = Assessment.of(Catalogue.all(), Level.L2, STORE);
        assertFalse(l2.byChapter().containsKey("Cryptography"));
        assertFalse(l2.byChapter().containsKey("Data Protection"));
        assertTrue(Assessment.of(Catalogue.all(), Level.L3, STORE).byChapter().containsKey("Cryptography"));
    }
}
