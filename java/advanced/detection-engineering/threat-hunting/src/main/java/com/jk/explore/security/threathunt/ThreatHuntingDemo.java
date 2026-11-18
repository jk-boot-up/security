package com.jk.explore.security.threathunt;

import com.jk.explore.security.threathunt.Events.Dns;
import com.jk.explore.security.threathunt.Events.Login;
import com.jk.explore.security.threathunt.Events.Week;
import com.jk.explore.security.threathunt.Hunts.Baseline;
import com.jk.explore.security.threathunt.Hunts.Gaps;
import com.jk.explore.security.threathunt.Hunts.Lead;
import com.jk.explore.security.threathunt.Hunts.Volume;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

/** Runs four hunts over a seeded week at a bank, judges every lead against the planted truth, and turns the beacon hunt into a rule. */
public class ThreatHuntingDemo {

    static final long SEED = 20261023L;
    static final String BEACON = "wks-014 -> cdn-sync.example.invalid";
    static final String HEARTBEAT = "monitoring.partner.example";
    /** What really happened in the week: the leads that are attacks. */
    static final Set<String> ATTACK = Set.of(BEACON, "winword -> powershell", "powershell -> whoami", "srv-03 on day 5", "svc-backup from 203.0.113.50");

    public static void main(String[] args) {
        System.out.print(run());
    }

    static String f(String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }

    static long attacks(List<Lead> leads) {
        return leads.stream().filter(l -> ATTACK.contains(l.subject())).count();
    }

    static String verdict(Lead l) {
        return ATTACK.contains(l.subject()) ? "ATTACK" : "not an attack";
    }

    static void leads(StringBuilder out, List<Lead> leads) {
        leads.stream().sorted(Hunts.bySubject()).forEach(l -> out.append(f("    %-38s %s%n", l.subject(), verdict(l))));
    }

    static String clock(long at) {
        return f("day %d %02d:%02d", at / Generator.DAY, at % Generator.DAY / 3600, at % 3600 / 60);
    }

    static String hours(SortedSet<Integer> h) {
        return h.size() > 2 && h.last() - h.first() == h.size() - 1 ? f("%02d to %02d", h.first(), h.last()) : String.join(", ", h.stream().map(x -> f("%02d", x)).toList());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        Week week = Generator.week(SEED, true);
        Map<String, List<Long>> times = Hunts.callTimes(week);
        Map<String, Integer> hostsOf = Hunts.hostsPerDomain(week);

        out.append("ONE. The week.\n");
        out.append(f("  THE WEEK (seed %d, made by Generator):%n", SEED));
        out.append("    hosts    40 workstations, 5 servers\n");
        out.append("    users    60 people, 1 service account (svc-backup)\n");
        out.append(f("    records  %d name lookups%n", week.dns().size()));
        out.append(f("             %d process starts%n", week.procs().size()));
        out.append(f("             %d logins%n", week.logins().size()));
        List<Long> beacon = times.get(BEACON);
        long upload = week.dns().stream().filter(d -> d.domain().equals("files-share.example.invalid")).mapToLong(Dns::bytesOut).sum();
        Login stolen = week.logins().stream().filter(l -> l.ip().equals("203.0.113.50")).findFirst().orElseThrow();
        out.append("  THE PLANTED ATTACKS (what the leads are judged by):\n");
        out.append(f("    1 beacon    wks-014, %d calls, days %d to %d%n", beacon.size(), beacon.getFirst() / Generator.DAY, beacon.getLast() / Generator.DAY));
        out.append("    2 shell     wks-014  winword -> powershell -> whoami\n");
        out.append(f("    3 upload    srv-03   %d MB out on day 5, at night%n", upload / 1_000_000));
        out.append(f("    4 account   svc-backup from %s, %s%n", stolen.ip(), clock(stolen.at())));
        out.append("    5 task      a scheduled task on a server: in no log\n");
        out.append("  THE LOOK-ALIKES (benign, planted on purpose):\n");
        out.append("    srv-02   a heartbeat to " + HEARTBEAT + "\n");
        out.append("    srv-02   600 MB to a partner every night\n");
        out.append("    srv-04   a 300 MB patch download on day 6\n");
        out.append("    wks-007  an administrator runs cmd -> ipconfig twice\n");
        out.append("    user12   a manager logs in from a hotel on day 4\n");

        out.append("TWO. Hunt 1: a timer in the lookups.\n");
        out.append("  THE HYPOTHESIS:\n");
        out.append("    malware calls home on a timer: many calls, even gaps\n");
        out.append("  HOW EVEN THE GAPS ARE (variation = sd / mean of the gaps):\n");
        out.append("    host -> domain                       calls  mean    sd   var\n");
        for (String pair : List.of(BEACON, "srv-02 -> " + HEARTBEAT, "wks-001 -> updates.vendor.example", "wks-001 -> news.example")) {
            Gaps g = Hunts.gaps(times.get(pair));
            out.append(f("    %-36s %5d %5.0f %5.0f  %4.2f%n", pair, g.calls(), g.mean(), g.sd(), g.variation()));
        }
        List<Lead> regular = Hunts.beaconing(week, 50, 0.3, 100, Set.of());
        Map<String, Integer> byDomain = new TreeMap<>();
        regular.forEach(l -> byDomain.merge(l.subject().substring(l.subject().indexOf("> ") + 2), 1, Integer::sum));
        out.append("  BEFORE RARITY (50+ calls, variation under 0.30):\n");
        out.append(f("    %d leads, by domain:%n", regular.size()));
        byDomain.forEach((domain, n) -> out.append(f("    %3d %-5s  %-27s called by %2d %s%n", n, n == 1 ? "lead" : "leads", domain, hostsOf.get(domain), hostsOf.get(domain) == 1 ? "host" : "hosts")));
        List<Lead> rare = Hunts.beaconing(week, 50, 0.3, 3, Set.of());
        out.append(f("  AFTER RARITY (and at most 3 hosts call the domain): %d leads%n", rare.size()));
        leads(out, rare);
        List<Lead> strict = Hunts.beaconing(week, 50, 0.01, 3, Set.of());
        out.append(f("  TOO STRICT (variation under 0.01): %d lead%n", strict.size()));
        leads(out, strict);
        out.append(f("    the beacon's 10%% jitter gives %.2f: it slips through%n", Hunts.gaps(beacon).variation()));
        List<Lead> allowlisted = Hunts.beaconing(week, 50, 0.3, 3, Set.of(HEARTBEAT));
        out.append(f("  AFTER THE ALLOWLIST (the heartbeat judged benign): %d lead%n", allowlisted.size()));
        leads(out, allowlisted);

        out.append("THREE. Hunts 2 and 3: pairs and accounts.\n");
        out.append("  HUNT 2, THE HYPOTHESIS:\n");
        out.append("    an intruder runs programs that nobody else runs\n");
        Map<String, Integer> counts = Hunts.pairCounts(week);
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()));
        out.append(f("  THE COUNTS (parent -> child, %d pairs, a week):%n", counts.size()));
        for (int i = 0; i < sorted.size(); i++) {
            if (i < 3 || i >= sorted.size() - 3) {
                out.append(f("    %-24s %5d%n", sorted.get(i).getKey(), sorted.get(i).getValue()));
            } else if (i == 3) {
                out.append("    ...\n");
            }
        }
        List<Lead> pairs = Hunts.rarePairs(week, 2);
        out.append(f("  THE LEADS (seen at most twice): %d, %d attacks%n", pairs.size(), attacks(pairs)));
        leads(out, pairs);
        out.append("  HUNT 3, THE HYPOTHESIS:\n");
        out.append("    a stolen account logs in at a new hour from a new place\n");
        Map<String, Baseline> base = Hunts.baselines(week);
        out.append("  THE BASELINE (days 0 to 3), two of the 61 accounts:\n");
        for (String user : List.of("svc-backup", "user12")) {
            Baseline b = base.get(user);
            out.append(f("    %-10s  from %-10s  at hours %s%n", user, String.join(", ", b.ips()), hours(b.hours())));
        }
        List<Lead> logins = Hunts.oddLogins(week);
        out.append("  THE LOGINS FROM A NEW ADDRESS (days 4 to 6):\n");
        for (Lead l : logins.stream().sorted(Hunts.bySubject()).toList()) {
            Login login = week.logins().stream().filter(x -> l.subject().equals(x.user() + " from " + x.ip())).findFirst().orElseThrow();
            out.append(f("    %-28s %s, hour %02d%n", l.subject(), clock(login.at()).substring(0, 5), login.hour()));
            out.append(f("      %s%n", l.why()));
        }
        List<Lead> odd = logins.stream().filter(l -> l.why().contains("not been seen")).toList();
        out.append(f("  THE LEADS (a new address AND a new hour): %d%n", odd.size()));
        leads(out, odd);

        out.append("FOUR. Hunt 4 and the scoreboard.\n");
        out.append("  HUNT 4, THE HYPOTHESIS:\n");
        out.append("    data leaves in bulk, far above the host's own normal\n");
        Map<String, long[]> perDay = Hunts.bytesPerDay(week);
        out.append("  HOW EACH HOST'S NORMAL IS WORKED OUT (MB out per day):\n");
        out.append("    host     d0  d1  d2  d3  d4   d5   d6  median   MAD  limit\n");
        for (String host : List.of("srv-03", "srv-04")) {
            Volume v = Hunts.volumeOf(perDay.get(host), 6);
            StringBuilder row = new StringBuilder(f("    %-7s", host));
            for (int d = 0; d < 7; d++) {
                row.append(f(d < 5 ? "%4d" : "%5d", Math.round(v.perDay()[d] / 1e6)));
            }
            out.append(row).append(f("  %6.2f %5.2f %6.2f%n", v.median() / 1e6, v.mad() / 1e6, v.limit() / 1e6));
        }
        out.append("    limit = median + 6 x MAD; a lead is also over 50 MB\n");
        Volume partner = Hunts.volumeOf(perDay.get("srv-02"), 6);
        out.append(f("    srv-02 sends %.1f MB every day: its own median, no lead%n", partner.median() / 1e6));
        List<Lead> volume = Hunts.volume(week, 6);
        out.append(f("  THE LEADS: %d, %d attack%n", volume.size(), attacks(volume)));
        leads(out, volume);
        out.append("  THE SCOREBOARD (every lead judged by a person):\n");
        out.append("    hunt                    leads  attacks  not\n");
        List<String> all = new ArrayList<>();
        String[] names = {"1 timer, allowlisted", "2 rare pairs", "3 odd logins", "4 bulk volume"};
        List<List<Lead>> hunts = List.of(allowlisted, pairs, odd, volume);
        for (int i = 0; i < 4; i++) {
            List<Lead> h = hunts.get(i);
            h.forEach(l -> all.add(l.subject()));
            out.append(f("    %-22s %6d %8d %4d%n", names[i], h.size(), attacks(h), h.size() - attacks(h)));
        }
        long found = ATTACK.stream().filter(all::contains).count();
        out.append(f("    %-22s %6d %8d %4d%n", "total", all.size(), found, all.size() - found));
        out.append(f("  THE ATTACKS FOUND: 4 of the 4 that the logs show (%d events)%n", found));
        out.append("  NOT FOUND: attack 5, the scheduled task: no log records it\n");
        out.append("    a gap in the data, not in the hunt\n");

        out.append("FIVE. From a hunt to a rule.\n");
        out.append("  THE RULE: hunt 1, run every week with the same limits\n");
        out.append("    50+ calls, variation under 0.30, at most 3 hosts\n");
        Week clean = Generator.week(SEED + 1, false);
        out.append(f("  BEFORE THE ALLOWLIST, a clean week (seed %d):%n", SEED + 1));
        List<Lead> before = Hunts.beaconing(clean, 50, 0.3, 3, Set.of());
        out.append(f("    %d alert%n", before.size()));
        before.forEach(l -> out.append(f("    %-38s false alarm%n", l.subject())));
        out.append("  AFTER THE ALLOWLIST (" + HEARTBEAT + "):\n");
        out.append(f("    the clean week   %d alerts%n", Hunts.beaconing(clean, 50, 0.3, 3, Set.of(HEARTBEAT)).size()));
        List<Lead> after = Hunts.beaconing(week, 50, 0.3, 3, Set.of(HEARTBEAT));
        out.append(f("    the attack week  %d alert%n", after.size()));
        after.forEach(l -> out.append(f("    %-38s true alarm%n", l.subject())));
        out.append("  THE BILL:\n");
        out.append("    a hunt gives leads, not verdicts: each costs a person's time\n");
        out.append("    a hunt can only look at data that was collected\n");
        out.append("    a hunt that worked becomes a rule, with an allowlist and tests\n");
        return out.toString();
    }
}
