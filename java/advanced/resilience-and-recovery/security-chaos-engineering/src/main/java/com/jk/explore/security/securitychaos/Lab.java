package com.jk.explore.security.securitychaos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Runs the experiments against one version of the service, through Toxiproxy, and keeps the scoreboard. */
final class Lab {

    static final int DEADLINE_MS = 800;
    static final int PROBE_ROUNDS = 4;
    static final int PAYMENTS = 6;
    static final int BURST = 40;
    static final int AUDIT_BURST = 20;
    static final int AUDIT_SMALL = 4;

    /** The result of one experiment. */
    record Result(Experiment experiment, boolean steadyState, String observed, boolean controlHeld, String failure,
                  String alert, boolean alertHeld) {

        int held() {
            return (controlHeld ? 1 : 0) + (alertHeld ? 1 : 0);
        }
    }

    private final Backends backends;
    private final Toxiproxy proxy;

    Lab(Backends backends, Toxiproxy proxy) {
        this.backends = backends;
        this.proxy = proxy;
    }

    private String url(String name) {
        return "http://127.0.0.1:" + proxy.port(name);
    }

    private BankPayments.Endpoints endpoints() {
        return new BankPayments.Endpoints(url(Backends.AUTHZ), url(Backends.FRAUD), url(Backends.AUDIT),
                url(Backends.RATE), url(Backends.OCSP));
    }

    /** Runs every experiment; the hardened flag picks the version of the service. */
    Map<Experiment, Result> runAll(boolean hardened, StringBuilder out) throws IOException, InterruptedException {
        Map<Experiment, Result> results = new LinkedHashMap<>();
        for (Experiment e : Experiment.all()) {
            Result r = run(e, hardened);
            results.put(e, r);
            out.append("--- ").append(e.label()).append(" ").append(e.title()).append('\n');
            out.append("hypothesis: ").append(e.hypothesis()).append('\n');
            out.append("injected: ").append(e.dependency()).append(", ").append(e.fault().description()).append('\n');
            out.append("steady state before: ").append(r.steadyState() ? "yes" : "NO").append('\n');
            out.append("observed: ").append(r.observed()).append('\n');
            out.append("control: ").append(r.controlHeld() ? "held" : "BROKEN, " + r.failure()).append('\n');
            out.append("alert: ").append(r.alert()).append(" -> ").append(r.alertHeld() ? "held" : "BROKEN").append('\n');
        }
        return results;
    }

    private Result run(Experiment e, boolean hardened) throws IOException, InterruptedException {
        proxy.reset();
        backends.reset();
        Path dir = Files.createTempDirectory("sc-audit");
        try (Http http = new Http(DEADLINE_MS); Http probes = new Http(DEADLINE_MS);
             AuditTrail audit = hardened ? new SpooledAudit(http, url(Backends.AUDIT), dir.resolve("journal.log"))
                     : new DroppingAudit(http, url(Backends.AUDIT))) {
            BankPayments service = new BankPayments(endpoints(), hardened, http, audit);
            boolean steady = steadyState(service, audit);
            backends.reset();

            proxy.inject(e.dependency(), e.fault());
            Observation obs = observe(e, service, audit);
            Monitor monitor = new Monitor(healthUrls(), probes);
            if (hardened) {
                for (int i = 0; i < PROBE_ROUNDS; i++) {
                    monitor.round();
                }
            }
            boolean auditScenario = e.scenario() == Experiment.Scenario.AUDIT_BURST || e.scenario() == Experiment.Scenario.AUDIT_SMALL;
            if (auditScenario) {
                audit.flush(); // let the sender give up on everything it holds while the fault is still in place
            }
            proxy.reset();
            if (auditScenario) {
                audit.flush();
                obs = auditObservation(e, obs.approved, obs.total);
            }
            return result(e, steady, obs, monitor, hardened);
        } finally {
            try (var files = Files.list(dir)) {
                for (Path p : files.toList()) {
                    Files.deleteIfExists(p);
                }
            }
            Files.deleteIfExists(dir);
        }
    }

    private Map<String, String> healthUrls() {
        Map<String, String> m = new LinkedHashMap<>();
        for (String name : backends.names()) {
            m.put(name, url(name) + "/health");
        }
        return m;
    }

    /** Four ordinary payments with every dependency healthy: two good ones, a risky one and a revoked certificate. */
    private boolean steadyState(BankPayments service, AuditTrail audit) {
        boolean ok = service.pay(new Payment("s1", "std-1", 120, "ok-1")).approved();
        ok &= service.pay(new Payment("s2", "std-2", 80, "ok-1")).approved();
        ok &= !service.pay(new Payment("s3", "std-3", 9000, "ok-1")).approved();
        ok &= !service.pay(new Payment("s4", "std-4", 120, "rev-7")).approved();
        audit.flush();
        return ok && backends.auditCount() == 4;
    }

    private record Observation(int approved, int total, int unscreened, int reviews, int lost) {
    }

    private Observation observe(Experiment e, BankPayments service, AuditTrail audit) {
        return switch (e.scenario()) {
            case AUTHORIZATION -> pay(e, service, PAYMENTS, "ok-1", false);
            case FRAUD -> pay(e, service, PAYMENTS, "ok-1", false);
            case REVOCATION -> pay(e, service, PAYMENTS, "rev-7", false);
            case RATE_BURST -> pay(e, service, BURST, "ok-1", true);
            case AUDIT_BURST -> pay(e, service, AUDIT_BURST, "ok-1", false);
            case AUDIT_SMALL -> pay(e, service, AUDIT_SMALL, "ok-1", false);
        };
    }

    private Observation pay(Experiment e, BankPayments service, int count, String cert, boolean oneAccount) {
        int approved = 0;
        int unscreened = 0;
        int reviews = 0;
        for (int i = 0; i < count; i++) {
            String account = oneAccount ? "acc-burst" : "acc-" + e.number() + "-" + i;
            Decision d = service.pay(new Payment("e" + e.number() + "-" + i, account, 120, cert));
            if (d.approved()) {
                approved++;
                if (!d.screened()) {
                    unscreened++;
                }
            } else if (d.outcome() == Decision.Outcome.REVIEW) {
                reviews++;
            }
        }
        return new Observation(approved, count, unscreened, reviews, 0);
    }

    private Observation auditObservation(Experiment e, int approved, int total) {
        // Every payment makes one decision record; lost is what the sink never received.
        return new Observation(approved, total, 0, 0, total - backends.auditCount());
    }

    private Result result(Experiment e, boolean steady, Observation o, Monitor monitor, boolean hardened) {
        String observed;
        boolean held;
        String failure;
        switch (e.scenario()) {
            case AUTHORIZATION -> {
                observed = "approved " + o.approved + " of " + o.total;
                held = o.approved == 0;
                failure = "fails open";
            }
            case FRAUD -> {
                observed = "approved without a fraud score " + o.unscreened + " of " + o.total + ", held for review " + o.reviews;
                held = o.unscreened == 0;
                failure = "payments go unscreened";
            }
            case REVOCATION -> {
                observed = "approved with a revoked certificate " + o.approved + " of " + o.total;
                held = o.approved == 0;
                failure = "the check soft-fails";
            }
            case RATE_BURST -> {
                observed = "approved " + o.approved + " of " + o.total + " from one account, limit " + BankPayments.LIMIT;
                held = o.approved <= BankPayments.LIMIT;
                failure = "the limit disappears";
            }
            default -> {
                observed = "payments approved " + o.approved + " of " + o.total + ", audit records lost " + o.lost + " of " + o.total;
                held = o.lost == 0;
                failure = "audit records silently lost";
            }
        }
        boolean raised = monitor.alerted(e.dependency());
        boolean other = monitor.alerts().stream().anyMatch(a -> !a.dependency().equals(e.dependency()));
        String alert;
        boolean alertHeld;
        if (!hardened) {
            alert = "no monitor, none raised";
            alertHeld = !e.alertExpected();
        } else if (raised) {
            int n = monitor.alerts().stream().filter(a -> a.dependency().equals(e.dependency())).findFirst().orElseThrow().failedProbes();
            alert = "raised for " + e.dependency() + " after " + n + " failed probes";
            alertHeld = e.alertExpected() && !other && n <= Monitor.THRESHOLD;
        } else {
            alert = "none in " + PROBE_ROUNDS + " probe rounds";
            alertHeld = !e.alertExpected() && !other;
        }
        return new Result(e, steady, observed, held, failure, alert, alertHeld);
    }

    /** The one case the hardened version still accepts: a certificate revoked after its last good answer was cached. */
    String staleCacheRisk() throws IOException, InterruptedException {
        proxy.reset();
        backends.reset();
        try (Http http = new Http(DEADLINE_MS); AuditTrail audit = new SpooledAudit(http, url(Backends.AUDIT),
                Files.createTempFile("sc-journal", ".log"))) {
            BankPayments service = new BankPayments(endpoints(), true, http, audit);
            service.pay(new Payment("w1", "late-acc", 120, "late-8"));
            backends.revoke("late-8");
            proxy.inject(Backends.OCSP, Fault.DOWN);
            boolean approved = service.pay(new Payment("w2", "late-acc-2", 120, "late-8")).approved();
            proxy.reset();
            return "certificate revoked while the responder is down, last answer was good: approved " + (approved ? 1 : 0) + " of 1";
        }
    }
}
