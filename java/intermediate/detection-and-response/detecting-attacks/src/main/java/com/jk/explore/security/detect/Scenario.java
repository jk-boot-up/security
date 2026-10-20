package com.jk.explore.security.detect;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * One synthetic Monday for a company with 200 accounts: ordinary logins with a few typos, five VPN users, a traveller, two people who forgot their password after a
 * holiday, ticketed admin grants and one break-glass grant, and four real attacks. The random generator is seeded, so every run is the same.
 */
public final class Scenario {

    static final Instant DAY = Instant.parse("2026-03-02T00:00:00Z");
    static final City[] HOMES = {City.PARIS, City.BERLIN, City.LONDON, City.MADRID, City.NEW_YORK};
    static final String VPN_EGRESS = "198.18.0.1";
    static final Set<String> APPROVERS = Set.of("ops1", "ops2");

    private Scenario() {
    }

    static Instant at(int hour, int minute, int second) {
        return DAY.plusSeconds(hour * 3600L + minute * 60L + second);
    }

    static String user(int i) {
        return String.format("u%03d", i);
    }

    static City home(int i) {
        return i >= 20 && i <= 24 ? City.NEW_YORK : HOMES[i % HOMES.length];
    }

    static String homeIp(int i) {
        return "10." + (i % HOMES.length) + "." + (i / 250) + "." + (i % 250 + 1);
    }

    /** Each account's usual address from the previous weeks, which a takeover rule can compare with. */
    public static java.util.Map<String, Set<String>> history() {
        java.util.Map<String, Set<String>> h = new java.util.HashMap<>();
        for (int i = 1; i <= 200; i++) {
            h.put(user(i), Set.of(homeIp(i)));
        }
        return h;
    }

    public static List<Event> day() {
        SplittableRandom r = new SplittableRandom(42);
        List<Event> log = new ArrayList<>();

        for (int i = 1; i <= 200; i++) {
            int logins = 2 + r.nextInt(3);
            for (int k = 0; k < logins; k++) {
                int span = i == 42 || i == 88 || i == 117 ? 3 : 11; // the attacked accounts' own logins come in the morning
                Instant t = at(7 + r.nextInt(span), r.nextInt(60), r.nextInt(60));
                if (i == 30 || i == 33) {
                    continue; // these two have hand-written logins below
                }
                int typos = r.nextInt(100) < 3 ? (r.nextInt(5) == 0 ? 2 : 1) : 0;
                for (int x = 0; x < typos; x++) {
                    log.add(Event.login(t.minusSeconds(20L * (typos - x)), user(i), homeIp(i), home(i), false, null));
                }
                log.add(Event.login(t, user(i), homeIp(i), home(i), true, null));
            }
        }
        // two people back from holiday forget their password, ten minutes after 9:00
        for (int i : new int[] {10, 11}) {
            for (int s = 0; s < 6; s++) {
                log.add(Event.login(at(9, 10, 20 * s), user(i), homeIp(i), home(i), false, null));
            }
            log.add(Event.login(at(9, 13, 0), user(i), homeIp(i), home(i), true, null));
        }
        // five New York staff work through the company VPN, which shows as Frankfurt
        for (int i = 20; i <= 24; i++) {
            log.add(Event.login(at(8, 0, i), user(i), homeIp(i), City.NEW_YORK, true, null));
            log.add(Event.login(at(8, 30, i), user(i), VPN_EGRESS, City.FRANKFURT, true, null));
        }
        // a real traveller: Paris in the morning, New York in the evening
        log.add(Event.login(at(8, 0, 0), user(30), homeIp(30), City.PARIS, true, null));
        log.add(Event.login(at(17, 0, 0), user(30), "192.0.2.30", City.NEW_YORK, true, null));
        // a user in Madrid logs in at home and then on a phone whose carrier address is placed in Frankfurt by the geolocation database
        log.add(Event.login(at(8, 0, 31), user(33), homeIp(33), home(33), true, null));
        log.add(Event.login(at(8, 20, 31), user(33), "192.0.2.33", City.FRANKFURT, true, null));
        // approved admin grants, and one emergency grant without a ticket
        log.add(Event.grant(at(11, 0, 0), "ops1", user(50), "admin", "CHG-1001", null));
        log.add(Event.grant(at(14, 30, 0), "ops2", user(51), "admin", "CHG-1002", null));
        log.add(Event.grant(at(3, 0, 0), "ops1", user(60), "admin", "", null));

        // attack A: password spraying from one address, two guesses against each of 40 accounts, and one guess works
        for (int i = 101; i <= 140; i++) {
            for (int g = 0; g < 2; g++) {
                log.add(Event.login(at(10, 0, 0).plusSeconds((i - 101) * 25L + g * 600L / 2), user(i), "203.0.113.50", City.SINGAPORE, i == 117 && g == 1, "A"));
            }
        }
        // attack B: 30 guesses at one account in four minutes, then a success
        for (int s = 0; s < 30; s++) {
            log.add(Event.login(at(12, 0, 0).plusSeconds(s * 8L), user(42), "198.51.100.7", City.SINGAPORE, false, "B"));
        }
        log.add(Event.login(at(12, 4, 10), user(42), "198.51.100.7", City.SINGAPORE, true, "B"));
        // attack C: valid credentials used from Sydney 25 minutes after a login in Paris
        log.add(Event.login(at(15, 0, 0), user(88), homeIp(88), home(88), true, null));
        log.add(Event.login(at(15, 25, 0), user(88), "192.0.2.88", City.SYDNEY, true, "C"));
        // attack D: the account from attack A (u117) makes itself an administrator
        log.add(Event.grant(at(10, 40, 0), user(117), user(117), "admin", "", "D"));

        log.sort(Comparator.comparing(Event::time).thenComparing(Event::user));
        return log;
    }
}
