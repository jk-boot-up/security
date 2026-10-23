package com.jk.explore.security.privacyeng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class PrivacyDatabaseTest {

    @Test
    void theDataMapFindsTheTablesThatPointAtCustomers() throws Exception {
        try (Pg pg = new Pg()) {
            Seed.create(pg);
            List<String> map = Subject.dataMap(pg);
            assertTrue(map.containsAll(List.of("customers", "accounts", "transactions", "support_notes")));
            assertTrue(!map.contains("analytics_tx"));
        }
    }

    @Test
    void theRetentionJobKeepsWhatIsUnderLegalHoldAndNothingElseThatIsOld() throws Exception {
        try (Pg pg = new Pg()) {
            Seed.create(pg);
            Retention.Plan plan = Retention.run(pg, Seed.TODAY);
            assertEquals(plan.underHold(), pg.count("SELECT count(*) FROM transactions WHERE booked_on < ?", java.sql.Date.valueOf(Seed.TODAY.minusYears(7))));
            assertEquals(0, pg.count("SELECT count(*) FROM customers WHERE marketing_consent AND consent_date < ?", java.sql.Date.valueOf(Seed.TODAY.minusYears(2))));
        }
    }

    @Test
    void erasureLeavesMentionsInOtherPeoplesRecordsUntilTheyAreScrubbed() throws Exception {
        try (Pg pg = new Pg()) {
            Seed.create(pg);
            Pseudonyms ps = new Pseudonyms("k");
            ps.publish(pg);
            Subject s = new Subject(pg, 17);
            List<String> ids = s.erase(ps);
            assertTrue(Subject.remnants(pg, ids).getOrDefault("support_notes", 0L) >= 2);
            Subject.scrub(pg, ids);
            assertTrue(Subject.remnants(pg, ids).isEmpty());
            assertEquals(0, s.analyticsRows(ps));
        }
    }

    @Test
    void coarsenedValuesLeaveNoOneAloneWhileFullValuesLeaveAlmostEveryone() throws Exception {
        try (Pg pg = new Pg()) {
            Seed.create(pg);
            assertTrue(Anonymity.shareBelow(pg, "customers", Anonymity.FULL, 2) > 0.95);
            assertEquals(0.0, Anonymity.shareBelow(pg, "customers", Anonymity.COARSE, 2), 0);
        }
    }
}
