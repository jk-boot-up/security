package com.jk.explore.security.securitychaos;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Chaos experiments on the security controls of a bank payments service: steady state, injected failure, observation, verdict. */
public final class SecurityChaosDemo {

    private SecurityChaosDemo() {
    }

    public static void main(String[] args) throws Exception {
        System.out.print(run());
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
                out.append("--- the service under test\n");
                out.append("a bank payments service with 5 controls, each reached through its own Toxiproxy proxy: ")
                        .append("rate limit, certificate revocation, authorization, fraud score, audit trail\n");
                out.append("experiments: ").append(Experiment.all().size()).append(", each with 2 hypotheses (the control holds, the alert fires)\n");
                out.append("call deadline: bounded, failures injected: down, latency, hang, reset, trickle, slow close\n");

                out.append("=== first version\n");
                Map<Experiment, Lab.Result> first = lab.runAll(false, out);
                scoreboard(out, "first version", first);

                out.append("=== hardened version\n");
                Map<Experiment, Lab.Result> hardened = lab.runAll(true, out);
                scoreboard(out, "hardened version", hardened);

                out.append("--- the experiment scoreboard\n");
                for (Experiment e : Experiment.all()) {
                    Lab.Result a = first.get(e);
                    Lab.Result b = hardened.get(e);
                    out.append(String.format("%-4s %-40s first: control %-6s alert %-6s | hardened: control %-6s alert %-6s%n",
                            e.label(), e.title(), word(a.controlHeld()), word(a.alertHeld()), word(b.controlHeld()), word(b.alertHeld())));
                }
                out.append("hypotheses held: first version ").append(held(first)).append(" of ").append(2 * first.size())
                        .append(", hardened version ").append(held(hardened)).append(" of ").append(2 * hardened.size()).append('\n');

                out.append("--- what the hardened version still accepts\n");
                out.append(lab.staleCacheRisk()).append('\n');

                out.append("--- the bill\n");
                out.append("a control that cannot be reached must decide: the first version decided yes on authorization, fraud, rate limit and revocation\n");
                out.append("an audit buffer that drops is a control that fails silently: the lost records are only found by counting them\n");
                long needed = Experiment.all().stream().filter(Experiment::alertExpected).count();
                long missed = first.values().stream().filter(r -> r.experiment().alertExpected() && !r.alertHeld()).count();
                out.append("an alert is a control too: without probes, ").append(missed).append(" of the ").append(needed)
                        .append(" outages that needed one were never reported\n");
                out.append("fail closed has a price: payments wait in review or are refused while the dependency is down\n");
            }
        }
        return out.toString();
    }

    private static void scoreboard(StringBuilder out, String name, Map<Experiment, Lab.Result> results) {
        int broken = 2 * results.size() - held(results);
        out.append("scoreboard, ").append(name).append(": held ").append(held(results)).append(" of ").append(2 * results.size())
                .append(", broken ").append(broken).append('\n');
    }

    private static int held(Map<Experiment, Lab.Result> results) {
        return results.values().stream().mapToInt(Lab.Result::held).sum();
    }

    private static String word(boolean held) {
        return held ? "held" : "BROKEN";
    }
}
