package com.jk.explore.security.ir;

import java.util.List;

/** Runs the same incident three times in six numbered acts and prints exact, repeatable results. The attacker's behaviour is fixed, so only the response differs. */
public class IncidentDemo {

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();

        out.append("== 1. The attack ==\n");
        out.append("01:00 an attacker gets into web01 through a flaw in the application\n");
        out.append("01:05 reads the database password from the configuration\n");
        out.append("01:20 starts copying the customer table from db01, ").append(World.EXFIL_RATE).append(" rows a minute, ").append(World.CUSTOMERS).append(" customers in all\n");
        out.append("01:40 adds three ways back in: a cron job and an SSH key on web01, and a hidden admin account called svc-backup\n");

        out.append("\n== 2. What was prepared ==\n");
        for (Preparation p : List.of(Preparation.none(), Preparation.monitoringOnly(), Preparation.full())) {
            out.append(String.format("%-11s %d of 6 in place", name(p), p.score()));
            out.append(p.missing().isEmpty() ? "\n" : "; missing: " + String.join(", ", p.missing()) + "\n");
        }

        World a = Playbooks.panic();
        World b = Playbooks.improvised();
        World c = Playbooks.playbook();

        out.append("\n== 3. Response A: nothing prepared, and a panicked fix ==\n");
        a.timeline.forEach(l -> out.append(l).append('\n'));
        out.append("\n== 4. Response B: an alert, but no plan ==\n");
        b.timeline.forEach(l -> out.append(l).append('\n'));
        out.append("\n== 5. Response C: the playbook ==\n");
        c.timeline.forEach(l -> out.append(l).append('\n'));

        out.append("\n== 6. The review ==\n");
        List<Outcome> o = List.of(Outcome.of("A", a), Outcome.of("B", b), Outcome.of("C", c));
        out.append(String.format("%-34s %9s %9s %9s%n", "", "A", "B", "C"));
        out.append(row("found after", o.stream().map(x -> human(x.foundAfterMinutes())).toList()));
        out.append(row("rows copied before it was stopped", o.stream().map(x -> x.rowsCopiedBeforeStop() < 0 ? "never" : String.valueOf(x.rowsCopiedBeforeStop())).toList()));
        out.append(row("ways back in left open", o.stream().map(x -> String.valueOf(x.footholdsLeft().size())).toList()));
        out.append(row("evidence kept", o.stream().map(x -> yes(x.evidenceKept())).toList()));
        out.append(row("root cause known", o.stream().map(x -> yes(x.rootCauseKnown())).toList()));
        out.append(row("web01 out of service (minutes)", o.stream().map(x -> String.valueOf(x.outageMinutes())).toList()));
        out.append(row("the attacker came back", o.stream().map(x -> yes(x.attackerReturned())).toList()));
        out.append(row("recovered", o.stream().map(x -> yes(x.recovered())).toList()));
        return out.toString();
    }

    private static String name(Preparation p) {
        return p.equals(Preparation.none()) ? "nothing" : p.equals(Preparation.monitoringOnly()) ? "alert only" : "full";
    }

    private static String row(String label, List<String> values) {
        return String.format("%-34s %9s %9s %9s%n", label, values.get(0), values.get(1), values.get(2));
    }

    private static String human(int minutes) {
        return minutes >= 1440 ? (minutes / 1440) + " days" : minutes + " min";
    }

    private static String yes(boolean b) {
        return b ? "yes" : "no";
    }
}
