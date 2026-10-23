package com.jk.explore.security.threathunt;

import com.jk.explore.security.threathunt.Events.Week;
import com.jk.explore.security.threathunt.Hunts.Lead;
import java.util.List;
import java.util.Set;

public class ThreatHuntingDemo {

    static final long SEED = 20261023L;
    /** What really happened in the week: the leads that are attacks. */
    static final Set<String> ATTACK = Set.of("wks-014 -> cdn-sync.example.invalid", "winword -> powershell", "powershell -> whoami", "srv-03 on day 5", "svc-backup from 203.0.113.50");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String score(List<Lead> leads) {
        long real = leads.stream().filter(l -> ATTACK.contains(l.subject())).count();
        return leads.size() + " leads, " + real + " are attacks, " + (leads.size() - real) + " are not";
    }

    static String list(List<Lead> leads) {
        return String.join("; ", leads.stream().sorted(Hunts.bySubject()).map(l -> l.subject() + (ATTACK.contains(l.subject()) ? " [attack]" : " [not an attack]")).toList());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        Week week = Generator.week(SEED, true);

        out.append("--- the week\n");
        out.append("name lookups: " + week.dns().size() + ", process starts: " + week.procs().size() + ", logins: " + week.logins().size() + "\n");
        out.append("hosts: 45, users: 61; planted: 4 attacks (a beacon, a shell from a document, an upload in bulk, a stolen service account) and 1 more that no log records\n");

        out.append("--- hunt 1: a timer in the lookups\n");
        List<Lead> everything = Hunts.beaconing(week, 50, 0.3, 100, Set.of());
        out.append("regular calls only: " + everything.size() + " leads, which is every host and every update check\n");
        List<Lead> rare = Hunts.beaconing(week, 50, 0.3, 3, Set.of());
        out.append("regular and rare (a domain that at most 3 hosts call): " + score(rare) + ": " + list(rare) + "\n");
        List<Lead> strict = Hunts.beaconing(week, 50, 0.01, 3, Set.of());
        out.append("too strict a limit on how regular (a variation under 1%): " + score(strict) + ", the beacon with 10% jitter " + (strict.stream().anyMatch(l -> ATTACK.contains(l.subject())) ? "is still there" : "slips through") + "\n");
        List<Lead> allowlisted = Hunts.beaconing(week, 50, 0.3, 3, Set.of("monitoring.partner.example"));
        out.append("after the analyst allowlists the partner's heartbeat: " + score(allowlisted) + "\n");

        out.append("--- hunt 2: programs nobody else runs\n");
        List<Lead> pairs = Hunts.rarePairs(week, 2);
        out.append("parent and child pairs seen at most twice in the week: " + score(pairs) + ": " + list(pairs) + "\n");

        out.append("--- hunt 3: accounts in odd places\n");
        List<Lead> logins = Hunts.oddLogins(week);
        List<Lead> odd = logins.stream().filter(l -> l.why().contains("not been seen")).toList();
        out.append("a new address after the first four days: " + score(logins) + "\n");
        out.append("a new address at an hour never seen as well: " + score(odd) + ": " + list(odd) + "\n");

        out.append("--- hunt 4: data leaving in bulk\n");
        List<Lead> volume = Hunts.volume(week, 6);
        out.append("a host-day of more than 50 MB and more than 6 deviations above that host's own median: " + score(volume) + ": " + list(volume) + "\n");

        out.append("--- the scoreboard\n");
        List<String> all = new java.util.ArrayList<>();
        for (List<Lead> leads : List.of(allowlisted, pairs, odd, volume)) {
            leads.forEach(l -> all.add(l.subject()));
        }
        long found = ATTACK.stream().filter(all::contains).count();
        out.append("leads to review: " + all.size() + ", attacks among them: " + found + " of " + ATTACK.size() + " events, covering 4 of the 4 attacks the logs can show\n");
        out.append("not found by any hunt: the fifth attack, a scheduled task kept on a server, because no log source records it (a gap in the data, not in the hunt)\n");

        out.append("--- from a hunt to a detection\n");
        Week clean = Generator.week(SEED + 1, false);
        out.append("the beacon hunt as a rule, on a clean week, with no allowlist: " + Hunts.beaconing(clean, 50, 0.3, 3, Set.of()).size() + " alert, a false one; with the allowlist: " + Hunts.beaconing(clean, 50, 0.3, 3, Set.of("monitoring.partner.example")).size() + "\n");
        out.append("the same rule on the attack week: " + Hunts.beaconing(week, 50, 0.3, 3, Set.of("monitoring.partner.example")).size() + " alert, a true one\n");

        out.append("--- the bill\n");
        out.append("a hunt produces leads, not verdicts: every lead costs a person's time, and the benign ones teach the allowlist\n");
        out.append("a hunt can only look at data that was collected\n");
        out.append("a hunt that worked becomes a rule, with its allowlist and its tests\n");
        return out.toString();
    }
}
