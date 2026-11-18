package com.jk.explore.security.threathunt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jk.explore.security.threathunt.Events.Dns;
import com.jk.explore.security.threathunt.Events.Login;
import com.jk.explore.security.threathunt.Events.Proc;
import com.jk.explore.security.threathunt.Events.Week;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HuntsTest {

    private static Week dnsOnly(List<Dns> dns) {
        return new Week(dns, List.of(), List.of());
    }

    @Test
    void aPerfectTimerIsALeadAndRandomCallsAreNot() {
        List<Dns> d = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            d.add(new Dns("h1", "timer.example", i * 300L, 10));
            d.add(new Dns("h1", "random.example", (i * 7919L * 31) % 100_000, 10));
        }
        List<Hunts.Lead> leads = Hunts.beaconing(dnsOnly(d), 50, 0.3, 3, Set.of());
        assertEquals(List.of("h1 -> timer.example"), leads.stream().map(Hunts.Lead::subject).toList());
    }

    @Test
    void aDomainThatManyHostsCallIsNotRare() {
        List<Dns> d = new ArrayList<>();
        for (int h = 0; h < 10; h++) {
            for (int i = 0; i < 100; i++) {
                d.add(new Dns("h" + h, "common.example", i * 300L, 10));
            }
        }
        assertEquals(10, Hunts.beaconing(dnsOnly(d), 50, 0.3, 100, Set.of()).size());
        assertEquals(0, Hunts.beaconing(dnsOnly(d), 50, 0.3, 3, Set.of()).size());
    }

    @Test
    void anAllowlistedDomainIsLeftOut() {
        List<Dns> d = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            d.add(new Dns("h1", "known.example", i * 300L, 10));
        }
        assertEquals(0, Hunts.beaconing(dnsOnly(d), 50, 0.3, 3, Set.of("known.example")).size());
    }

    @Test
    void theRarestParentChildPairsAreTheLeads() {
        List<Proc> p = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            p.add(new Proc("h", "explorer", "chrome", i));
        }
        p.add(new Proc("h", "winword", "powershell", 99));
        assertEquals(List.of("winword -> powershell"), Hunts.rarePairs(new Week(List.of(), p, List.of()), 2).stream().map(Hunts.Lead::subject).toList());
    }

    @Test
    void aNewAddressAtANewHourIsALeadAndTheUsualAddressIsNot() {
        List<Login> l = new ArrayList<>();
        l.add(new Login("u", "10.0.0.1", 9 * 3600));
        l.add(new Login("u", "10.0.0.1", 4 * 86400 + 9 * 3600));
        l.add(new Login("u", "9.9.9.9", 5 * 86400 + 3 * 3600));
        List<Hunts.Lead> leads = Hunts.oddLogins(new Week(List.of(), List.of(), l));
        assertEquals(1, leads.size());
        assertTrue(leads.get(0).why().contains("not been seen"));
    }

    @Test
    void aBigDayIsALeadOnlyAboveTheFloorAndTheHostsOwnNormal() {
        List<Dns> d = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            d.add(new Dns("h", "x.example", day * Generator.DAY, 1_000_000));
        }
        d.add(new Dns("h", "x.example", 3 * Generator.DAY + 5, 900_000_000L));
        List<Hunts.Lead> leads = Hunts.volume(dnsOnly(d), 6);
        assertEquals(List.of("h on day 3"), leads.stream().map(Hunts.Lead::subject).toList());
    }

    @Test
    void theMedianOfAnEvenAndAnOddList() {
        assertEquals(2.0, Hunts.median(new double[] {3, 1, 2}));
        assertEquals(2.5, Hunts.median(new double[] {4, 1, 2, 3}));
    }

    @Test
    void theGeneratorIsSeededAndTheAttacksAreOptional() {
        Week a = Generator.week(5, true);
        Week b = Generator.week(5, true);
        assertEquals(a.dns().size(), b.dns().size());
        assertTrue(Generator.week(5, false).dns().size() < a.dns().size());
        assertEquals(a.procs().size() - 2, Generator.week(5, false).procs().size());
    }

    @Test
    void theGapsAreWorkedOutAsAMeanAndAStandardDeviation() {
        Hunts.Gaps even = Hunts.gaps(List.of(0L, 300L, 600L, 900L));
        assertEquals(4, even.calls());
        assertEquals(300.0, even.mean());
        assertEquals(0.0, even.variation());
        Hunts.Gaps uneven = Hunts.gaps(List.of(0L, 100L, 400L));
        assertEquals(200.0, uneven.mean());
        assertEquals(100.0, uneven.sd());
        assertEquals(0.5, uneven.variation());
    }

    @Test
    void aBaselineHoldsTheAddressesAndHoursOfTheFirstFourDays() {
        List<Login> l = List.of(new Login("u", "10.0.0.1", 9 * 3600), new Login("u", "10.0.0.2", 86400 + 14 * 3600), new Login("u", "9.9.9.9", 4 * 86400 + 3 * 3600));
        Hunts.Baseline b = Hunts.baselines(new Week(List.of(), List.of(), l)).get("u");
        assertEquals(Set.of("10.0.0.1", "10.0.0.2"), b.ips());
        assertEquals(List.of(9, 14), List.copyOf(b.hours()));
    }

    @Test
    void aHostsLimitIsItsMedianPlusDeviationsTimesTheMad() {
        Hunts.Volume v = Hunts.volumeOf(new long[] {10, 12, 8, 10, 11, 9, 500}, 6);
        assertEquals(10.0, v.median());
        assertEquals(1.0, v.mad());
        assertEquals(16.0, v.limit());
    }

    @Test
    void theDemoFindsEveryAttackTheLogsCanShow() {
        String out = ThreatHuntingDemo.run();
        assertTrue(out.contains("THE ATTACKS FOUND: 4 of the 4 that the logs show (5 events)"));
    }
}
