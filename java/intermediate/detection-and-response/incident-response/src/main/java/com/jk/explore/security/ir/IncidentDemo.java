package com.jk.explore.security.ir;

import java.util.List;
import java.util.function.Function;

/**
 * Runs the same incident three times in six numbered acts and prints exact, repeatable results. The attacker's behaviour is fixed, so only the response differs.
 * Each response is shown as the state before it, the steps, and the state after it.
 */
public class IncidentDemo {

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        attack(out);
        prepared(out);

        World a = Playbooks.panic();
        World b = Playbooks.improvised();
        World c = Playbooks.playbook();
        response(out, "THREE. Response A: nothing prepared, and a panicked fix.", a);
        response(out, "FOUR. Response B: an alert, but no plan.", b);
        response(out, "FIVE. Response C: the playbook.", c);
        review(out, a, b, c);
        return out.toString();
    }

    private static void attack(StringBuilder out) {
        out.append("ONE. The attack.\n");
        out.append("  THE ATTACKER'S SCRIPT (the same in all three runs):\n");
        out.append("    01:00  gets into web01 through a flaw in the application\n");
        out.append("    01:05  reads the database password from the configuration\n");
        out.append("    ").append(hhmm(World.EXFIL_START)).append("  starts copying the customer table from db01\n");
        out.append("    01:40  adds three ways back in: cron job, SSH key, svc-backup\n");
        out.append("  HOW FAST THE DATA GOES:\n");
        out.append(String.format("    rate      %d rows a minute, from %s%n", World.EXFIL_RATE, hhmm(World.EXFIL_START)));
        out.append(String.format("    table     %d customers%n", World.CUSTOMERS));
        int minutes = World.CUSTOMERS / World.EXFIL_RATE;
        out.append(String.format("    all gone  %d / %d = %d minutes later, at %s%n", World.CUSTOMERS, World.EXFIL_RATE, minutes,
                hhmm(World.EXFIL_START + minutes)));
        out.append("  THE WAYS BACK IN, AND WHAT CLOSES EACH:\n");
        World.waysBackIn().forEach((way, closer) -> out.append(String.format("    %-34s %s%n", way, closer)));
    }

    private static void prepared(StringBuilder out) {
        List<Preparation> preps = List.of(Preparation.none(), Preparation.monitoringOnly(), Preparation.full());
        out.append("TWO. What was prepared.\n");
        out.append("  THE SIX THINGS, DECIDED BEFORE THE ATTACK:\n");
        out.append(String.format("    %-32s%8s%8s%8s%n", "", "nothing", "alert", "full"));
        for (String thing : Preparation.full().checklist().keySet()) {
            out.append(String.format("    %-32s%8s%8s%8s%n", thing, yes(preps.get(0).checklist().get(thing)), yes(preps.get(1).checklist().get(thing)),
                    yes(preps.get(2).checklist().get(thing))));
        }
        out.append(String.format("    %-32s%8s%8s%8s%n", "in place", preps.get(0).score() + " of 6", preps.get(1).score() + " of 6", preps.get(2).score() + " of 6"));
        out.append("  WHEN THE BREACH IS FOUND:\n");
        for (Preparation p : preps) {
            int found = new World(p).detectedAt;
            String how = found >= 1440 ? String.format("%d x 24 x 60 = %d min, %s", found / 1440, found, World.clockAt(found))
                    : String.format("%d min, %s", found, World.clockAt(found));
            out.append(String.format("    %-11s %-14s %s%n", name(p), p.monitoringOnDatabaseReads() ? "by the alert" : "by a customer", how));
        }
    }

    private static void response(StringBuilder out, String title, World w) {
        World before = new World(w.prep);
        new Responder(before).detect();
        before.timeline.clear();
        out.append(title).append('\n');
        out.append("  BEFORE THE RESPONSE (").append(before.clock()).append("):\n");
        out.append("    rows copied   ").append(copied(before)).append('\n');
        out.append(String.format("    ways back in  %d of 5 open%n", before.footholdsLeft().size()));
        out.append("  THE RESPONSE (day ").append(before.clock().charAt(4)).append("):\n");
        w.timeline.forEach(l -> out.append("    ").append(l).append('\n'));
        out.append("  AFTER THE RESPONSE:\n");
        out.append("    copying stopped  ").append(w.stoppedAt < 0 ? "never: all " + World.CUSTOMERS + " rows were gone"
                : String.format("(%s - %s) x %d = %d rows", hhmm(w.stoppedAt), hhmm(World.EXFIL_START), World.EXFIL_RATE, w.rowsWhenStopped)).append('\n');
        out.append(String.format("    ways back in     %d of 5 open%n", w.footholdsLeft().size()));
        w.footholdsLeft().forEach(f -> out.append("      ").append(f).append('\n'));
        out.append(String.format("    evidence         memory %s, disk %s, logs %s%n", yes(w.memoryCaptured), yes(w.diskCaptured), yes(w.logsCollected)));
        out.append("    root cause       ").append(w.scoped ? "known: " + w.prep.logRetentionDays() + " days of central logs reach 01:00"
                : "unknown: no central logs").append('\n');
        int outage = w.reconnectedAt - w.outageStart;
        out.append(String.format("    out of service   %s to %s = %d min%n", hhmm(w.outageStart), hhmm(w.reconnectedAt), outage));
        out.append("    the attacker     ").append(w.returnedVia == null ? "did not come back" : "came back: " + w.returnedVia).append('\n');
    }

    private static void review(StringBuilder out, World a, World b, World c) {
        List<Outcome> o = List.of(Outcome.of("A", a), Outcome.of("B", b), Outcome.of("C", c));
        out.append("SIX. The review.\n");
        out.append("  THE THREE RESPONSES SIDE BY SIDE:\n");
        out.append(String.format("    %-28s%8s%8s%8s%n", "", "A", "B", "C"));
        row(out, "found after", o, x -> human(x.foundAfterMinutes()));
        row(out, "rows copied before the stop", o, x -> x.rowsCopiedBeforeStop() < 0 ? "never" : String.valueOf(x.rowsCopiedBeforeStop()));
        row(out, "ways back in left open", o, x -> String.valueOf(x.footholdsLeft().size()));
        row(out, "evidence kept", o, x -> yes(x.evidenceKept()));
        row(out, "root cause known", o, x -> yes(x.rootCauseKnown()));
        row(out, "web01 out of service (min)", o, x -> String.valueOf(x.outageMinutes()));
        row(out, "the attacker came back", o, x -> yes(x.attackerReturned()));
        row(out, "recovered", o, x -> yes(x.recovered()));
        int k = 0;
        while (b.actions.get(k).equals(c.actions.get(k))) {
            k++;
        }
        out.append("  WHERE B AND C PART WAYS:\n");
        out.append("    the same      ").append(String.join(", ", b.actions.subList(0, k))).append('\n');
        out.append(String.format("    B, step %d     %s%n", k + 1, b.timeline.get(k).substring(0, 27).strip()));
        out.append(String.format("    C, step %d     %s%n", k + 1, c.timeline.get(k).substring(0, 27).strip()));
    }

    private static String copied(World w) {
        if (w.exfiltrated >= World.CUSTOMERS) {
            return String.format("%d of %d, all gone since %s", w.exfiltrated, World.CUSTOMERS, World.clockAt(World.EXFIL_START + World.CUSTOMERS / World.EXFIL_RATE));
        }
        return String.format("(%s - %s) x %d = %d of %d", w.time(), hhmm(World.EXFIL_START), World.EXFIL_RATE, w.exfiltrated, World.CUSTOMERS);
    }

    private static String hhmm(int minute) {
        return World.clockAt(minute).substring(6);
    }

    private static String name(Preparation p) {
        return p.equals(Preparation.none()) ? "nothing" : p.equals(Preparation.monitoringOnly()) ? "alert only" : "full";
    }

    private static void row(StringBuilder out, String label, List<Outcome> o, Function<Outcome, String> value) {
        out.append(String.format("    %-28s%8s%8s%8s%n", label, value.apply(o.get(0)), value.apply(o.get(1)), value.apply(o.get(2))));
    }

    private static String human(int minutes) {
        return minutes >= 1440 ? (minutes / 1440) + " days" : minutes + " min";
    }

    private static String yes(boolean b) {
        return b ? "yes" : "no";
    }
}
