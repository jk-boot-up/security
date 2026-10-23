package com.jk.explore.security.certlife;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FleetTest {

    private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");

    private static Instant in(double days) {
        return NOW.plusSeconds((long) (days * 86400));
    }

    @Test
    void bucketsFollowTheTimeLeftOnAFixedClock() {
        assertEquals(Fleet.Bucket.EXPIRED, Fleet.bucket(in(-0.5), NOW));
        assertEquals(Fleet.Bucket.EXPIRED, Fleet.bucket(NOW, NOW));
        assertEquals(Fleet.Bucket.UNDER_7_DAYS, Fleet.bucket(in(6.9), NOW));
        assertEquals(Fleet.Bucket.UNDER_30_DAYS, Fleet.bucket(in(7), NOW));
        assertEquals(Fleet.Bucket.UNDER_30_DAYS, Fleet.bucket(in(29.9), NOW));
        assertEquals(Fleet.Bucket.OK, Fleet.bucket(in(30), NOW));
    }

    @Test
    void countsAddUpToTheInventory() {
        List<Fleet.Entry> fleet = List.of(new Fleet.Entry("a", in(-3)), new Fleet.Entry("b", in(2)), new Fleet.Entry("c", in(3)), new Fleet.Entry("d", in(90)));
        Map<Fleet.Bucket, Integer> counts = Fleet.count(fleet, NOW);
        assertEquals(1, counts.get(Fleet.Bucket.EXPIRED));
        assertEquals(2, counts.get(Fleet.Bucket.UNDER_7_DAYS));
        assertEquals(0, counts.get(Fleet.Bucket.UNDER_30_DAYS));
        assertEquals(1, counts.get(Fleet.Bucket.OK));
    }

    @Test
    void withoutAMonitorEveryExpiryInTheWindowIsAnOutage() {
        List<Fleet.Entry> fleet = List.of(new Fleet.Entry("in window", in(20.5)), new Fleet.Entry("later", in(200)), new Fleet.Entry("gone", in(-5)));
        Fleet.Model m = Fleet.model(fleet, NOW, 60, 0, 0, false);
        assertEquals(1, m.expiringInHorizon());
        assertEquals(1, m.outages());
    }

    @Test
    void anAlertOnlyHelpsWhenTheOwnerHasTheTimeToAct() {
        List<Fleet.Entry> fleet = new ArrayList<>();
        for (int d = 0; d < 20; d++) {
            fleet.add(new Fleet.Entry("c" + d, in(d + 0.5)));
        }
        Fleet.Model quick = Fleet.model(fleet, NOW, 60, 7, 2, true);
        Fleet.Model slow = Fleet.model(fleet, NOW, 60, 7, 10, true);
        assertEquals(20, quick.expiringInHorizon());
        assertEquals(2, quick.outages());
        assertEquals(18, quick.prevented());
        assertEquals(20, slow.outages());
        assertEquals(10, Fleet.model(fleet, NOW, 60, 30, 10, true).outages(), "the ten that were already under 10 days at the first scan");
    }

    @Test
    void anEarlierAlertPreventsMore() {
        List<Fleet.Entry> fleet = new ArrayList<>();
        for (int d = 0; d < 40; d++) {
            fleet.add(new Fleet.Entry("c" + d, NOW.plus(d, ChronoUnit.DAYS).plusSeconds(43200)));
        }
        assertEquals(10, Fleet.model(fleet, NOW, 60, 30, 10, true).outages());
        assertEquals(40, Fleet.model(fleet, NOW, 60, 7, 10, true).outages());
    }
}
