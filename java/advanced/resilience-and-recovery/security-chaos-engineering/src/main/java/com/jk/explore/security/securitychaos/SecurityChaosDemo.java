package com.jk.explore.security.securitychaos;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Chaos experiments on the security controls of a bank payments service: steady state, injected failure, observation, verdict. */
public final class SecurityChaosDemo {

    private SecurityChaosDemo() {
    }

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** The whole demo as text; the same on every run. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (Backends backends = new Backends("127.0.0.1")) {
            Map<String, Integer> upstream = new LinkedHashMap<>();
            for (String name : backends.names()) {
                upstream.put(name, backends.port(name));
            }
            try (Toxiproxy toxi = new Toxiproxy(upstream)) {
                Lab lab = new Lab(backends, toxi);
                Map<Experiment, Lab.Result> first = lab.runAll(false);
                Map<Experiment, Lab.Result> hardened = lab.runAll(true);
                int stale = lab.staleCacheRisk();
                setup(out, first, hardened);
                group(out, "TWO. Authorization: down, slow, reset.", first, hardened, Experiment.Scenario.AUTHORIZATION,
                        "an unanswered call is read as allow", "an unanswered call is a deny");
                group(out, "THREE. The fraud score: a hang and a trickle.", first, hardened, Experiment.Scenario.FRAUD,
                        "no score is read as a clean score", "no score: held for a person to review");
                trickle(out);
                audit(out, first, hardened);
                limitAndRevocation(out, first, hardened);
                alerts(out, first, hardened);
                scoreboard(out, first, hardened, stale);
            }
        }
        return out.toString();
    }

    private static void setup(StringBuilder out, Map<Experiment, Lab.Result> first, Map<Experiment, Lab.Result> hardened) {
        line(out, "ONE. The service, the faults and the steady state.");
        line(out, "  THE CONTROLS (each one a call to a dependency):");
        line(out, String.format("    %-14s %-14s %s", "control", "dependency", "first version, when it fails"));
        line(out, String.format("    %-14s %-14s %s", "rate limit", Backends.RATE, "no limit"));
        line(out, String.format("    %-14s %-14s %s", "revocation", Backends.OCSP, "not revoked (soft fail)"));
        line(out, String.format("    %-14s %-14s %s", "authorization", Backends.AUTHZ, "allow"));
        line(out, String.format("    %-14s %-14s %s", "fraud score", Backends.FRAUD, "a clean score"));
        line(out, String.format("    %-14s %-14s %s", "audit trail", Backends.AUDIT, "drop the record"));
        line(out, "  THE FAULTS (Toxiproxy " + Toxiproxy.IMAGE.substring(Toxiproxy.IMAGE.indexOf(':') + 1) + ", one proxy per dependency):");
        for (Fault f : Fault.values()) {
            line(out, String.format("    %-11s %s", f.label(), f.description()));
        }
        line(out, "  THE DEADLINE: " + Lab.DEADLINE_MS + " ms for the whole call, body included");
        line(out, "  THE STEADY STATE (before every experiment, both versions):");
        line(out, String.format("    %-4s %6s  %-6s %s", "", "amount", "cert", "decision"));
        List<Decision> steady = first.values().iterator().next().steady();
        for (int i = 0; i < Lab.STEADY.size(); i++) {
            Payment p = Lab.STEADY.get(i);
            Decision d = steady.get(i);
            line(out, String.format("    %-4s %6d  %-6s %s", p.id(), p.amount(), p.certSerial(),
                    d.outcome().name().toLowerCase(Locale.ROOT) + ": " + d.reason()));
        }
        line(out, "    audit records received: " + first.values().iterator().next().steadyAudit() + " of " + Lab.STEADY.size());
        long held = first.values().stream().filter(Lab.Result::steadyState).count() + hardened.values().stream().filter(Lab.Result::steadyState).count();
        line(out, "    steady state held before " + held + " of " + (first.size() + hardened.size()) + " runs");
    }

    /** The experiments of one scenario: their hypotheses, then the first version, then the hardened one. */
    private static void group(StringBuilder out, String title, Map<Experiment, Lab.Result> first, Map<Experiment, Lab.Result> hardened,
                              Experiment.Scenario scenario, String firstRule, String hardenedRule) {
        List<Experiment> es = Experiment.all().stream().filter(e -> e.scenario() == scenario).toList();
        line(out, title);
        hypotheses(out, es);
        line(out, "  THE FIRST VERSION (" + firstRule + "):");
        es.forEach(e -> observation(out, first.get(e)));
        line(out, "  THE HARDENED VERSION (" + hardenedRule + "):");
        es.forEach(e -> observation(out, hardened.get(e)));
    }

    private static void hypotheses(StringBuilder out, List<Experiment> es) {
        line(out, "  THE HYPOTHESES (" + experiments(es) + "):");
        for (Experiment e : es) {
            line(out, String.format("    %-4s %s", e.label(), e.hypothesis()));
        }
    }

    private static String experiments(List<Experiment> es) {
        return es.size() == 1 ? "one experiment" : es.size() + " experiments, one fault each";
    }

    /** One experiment's line: the fault, what was counted, and the verdict on the control. */
    private static void observation(StringBuilder out, Lab.Result r) {
        Experiment e = r.experiment();
        String counted = switch (e.scenario()) {
            case AUTHORIZATION -> "approved " + r.approved() + " of " + r.total();
            case FRAUD -> "unscreened " + r.unscreened() + " of " + r.total() + ", review " + r.reviews();
            case REVOCATION -> "revoked, approved " + r.approved() + " of " + r.total();
            case RATE_BURST -> "approved " + r.approved() + " of " + r.total();
            case AUDIT_BURST, AUDIT_SMALL -> "approved " + r.approved() + ", lost " + r.lost() + " of " + r.total();
        };
        line(out, String.format("    %-4s %-11s %-28s %s", e.label(), e.fault().label(), counted, r.controlHeld() ? "held" : "BROKEN"));
    }

    /** Why the trickle misses the deadline, worked out from the real size of an answer. */
    private static void trickle(StringBuilder out) {
        int bytes = Backends.bodyLength("score=10");
        double seconds = (double) bytes / Fault.TRICKLE_BYTES_PER_SECOND;
        line(out, "  HOW THE TRICKLE MISSES THE DEADLINE:");
        line(out, String.format(Locale.ROOT, "    an answer is %d bytes (the score line and a padding)", bytes));
        line(out, String.format(Locale.ROOT, "    %d bytes / %d bytes a second = %.1f s > %.1f s deadline", bytes,
                Fault.TRICKLE_BYTES_PER_SECOND, seconds, Lab.DEADLINE_MS / 1000.0));
    }

    private static void audit(StringBuilder out, Map<Experiment, Lab.Result> first, Map<Experiment, Lab.Result> hardened) {
        List<Experiment> es = Experiment.all().stream()
                .filter(e -> e.scenario() == Experiment.Scenario.AUDIT_BURST || e.scenario() == Experiment.Scenario.AUDIT_SMALL).toList();
        line(out, "FOUR. The audit trail: the sink down, then a slow close.");
        hypotheses(out, es);
        Experiment burst = es.getFirst();
        line(out, "  THE FIRST VERSION (a buffer of " + DroppingAudit.BUFFER + " in memory, drop what fails):");
        es.forEach(e -> observation(out, first.get(e)));
        line(out, "    " + burst.label() + ": dropped while the sink was down: " + first.get(burst).dropped() + ", nobody told");
        line(out, "  THE HARDENED VERSION (a journal on disk first, then send):");
        es.forEach(e -> observation(out, hardened.get(e)));
        Lab.Result h = hardened.get(burst);
        line(out, "    " + burst.label() + ": waiting in the journal while down: " + h.waiting() + ", sent when back: " + (h.total() - h.lost()));
    }

    private static void limitAndRevocation(StringBuilder out, Map<Experiment, Lab.Result> first, Map<Experiment, Lab.Result> hardened) {
        List<Experiment> es = Experiment.all().stream()
                .filter(e -> e.scenario() == Experiment.Scenario.RATE_BURST || e.scenario() == Experiment.Scenario.REVOCATION).toList();
        line(out, "FIVE. The rate limit and the revocation check.");
        hypotheses(out, es);
        line(out, "  THE FIRST VERSION (no limiter: no limit; no answer: good):");
        es.forEach(e -> observation(out, first.get(e)));
        line(out, "  THE HARDENED VERSION (count locally; no answer: refuse):");
        es.forEach(e -> observation(out, hardened.get(e)));
        Experiment burst = es.getFirst();
        int limit = BankPayments.LIMIT;
        line(out, "  HOW THE LOCAL COUNT HOLDS THE LIMIT (" + burst.label() + ", one account):");
        line(out, "    payments 1 to " + limit + ": count 1 to " + limit + " <= " + limit + ", approved");
        line(out, "    payments " + (limit + 1) + " to " + hardened.get(burst).total() + ": count " + (limit + 1) + " to "
                + hardened.get(burst).total() + " > " + limit + ", refused");
    }

    private static void alerts(StringBuilder out, Map<Experiment, Lab.Result> first, Map<Experiment, Lab.Result> hardened) {
        line(out, "SIX. Does anybody hear it?");
        line(out, "  THE MONITOR (hardened version only):");
        line(out, "    each round probes /health of the 5 dependencies");
        line(out, "    " + Monitor.THRESHOLD + " failed probes in a row raise one alert; " + Lab.PROBE_ROUNDS + " rounds");
        Experiment loud = Experiment.all().getFirst();
        Experiment quiet = Experiment.all().stream().filter(e -> !e.alertExpected()).findFirst().orElseThrow();
        trace(out, loud, hardened.get(loud).probes());
        trace(out, quiet, hardened.get(quiet).probes());
        long needed = Experiment.all().stream().filter(Experiment::alertExpected).count();
        long firstRaised = first.values().stream().filter(Lab.Result::alertRaised).count();
        long raised = hardened.values().stream().filter(r -> r.experiment().alertExpected() && r.alertRaised()).count();
        long falseAlarms = hardened.values().stream().filter(r -> !r.experiment().alertExpected() && r.alertRaised()).count();
        line(out, "  THE ALERTS (" + needed + " outages need one, " + quiet.label() + " needs none):");
        line(out, String.format("    %-16s raised %d of %d (no monitor)", "first version", firstRaised, needed));
        line(out, String.format("    %-16s raised %d of %d, after %d failed probes", "hardened", raised, needed, Monitor.THRESHOLD));
        line(out, String.format("    %-16s %d", "false alarms", falseAlarms));
    }

    /** The probes of one experiment's dependency, round by round. */
    private static void trace(StringBuilder out, Experiment e, List<Integer> probes) {
        line(out, "  THE PROBES IN " + e.label() + " (" + e.dependency() + ", " + e.fault().label() + "):");
        for (int round = 0; round < probes.size(); round++) {
            int n = probes.get(round);
            String what = n == 0 ? "answered" : "failed, " + n + " in a row";
            line(out, "    round " + (round + 1) + "  " + what + (n == Monitor.THRESHOLD ? "  -> ALERT" : ""));
        }
    }

    private static void scoreboard(StringBuilder out, Map<Experiment, Lab.Result> first, Map<Experiment, Lab.Result> hardened, int stale) {
        line(out, "SEVEN. The scoreboard, and what is left.");
        line(out, String.format("%-31s%-16s%s", "  THE SCOREBOARD", "first", "hardened"));
        line(out, String.format("    %-4s %-21s %-6s %-6s  %-6s %s", "", "", "ctl", "alert", "ctl", "alert"));
        for (Experiment e : Experiment.all()) {
            Lab.Result a = first.get(e);
            Lab.Result b = hardened.get(e);
            line(out, String.format("    %-4s %-21s %-6s %-6s  %-6s %s", e.label(), e.dependency() + " " + e.fault().label(),
                    word(a.controlHeld()), word(a.alertHeld()), word(b.controlHeld()), word(b.alertHeld())));
        }
        int total = 2 * first.size();
        line(out, "  HOW THE SCORE IS WORKED OUT:");
        line(out, "    " + first.size() + " experiments x 2 hypotheses (control, alert) = " + total);
        line(out, String.format("    %-16s held %d of %d, broken %d", "first version", held(first), total, total - held(first)));
        line(out, String.format("    %-16s held %d of %d, broken %d", "hardened", held(hardened), total, total - held(hardened)));
        line(out, "  THE GAP THE HARDENED VERSION STILL HAS:");
        line(out, "    a certificate revoked while the responder is down,");
        line(out, "    whose last cached answer was good: approved " + stale + " of 1");
        long needed = Experiment.all().stream().filter(Experiment::alertExpected).count();
        long missed = first.values().stream().filter(r -> r.experiment().alertExpected() && !r.alertHeld()).count();
        line(out, "  THE BILL:");
        line(out, "    no answer is still a decision: the first version said yes");
        line(out, "    a dropping audit buffer fails in silence; only a count finds it");
        line(out, "    without probes, " + missed + " of the " + needed + " outages were never reported");
        line(out, "    failing closed has a price: payments wait or are refused");
    }

    private static int held(Map<Experiment, Lab.Result> results) {
        return results.values().stream().mapToInt(Lab.Result::held).sum();
    }

    private static String word(boolean held) {
        return held ? "held" : "BROKEN";
    }
}
