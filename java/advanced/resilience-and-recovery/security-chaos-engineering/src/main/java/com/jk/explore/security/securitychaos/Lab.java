package com.jk.explore.security.securitychaos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runs the experiments against one version of the service, through Toxiproxy, and keeps the scoreboard. */
final class Lab {

    static final int DEADLINE_MS = 1500;
    static final int PROBE_ROUNDS = 4;
    static final int PAYMENTS = 6;
    static final int BURST = 40;
    static final int AUDIT_BURST = 20;
    static final int AUDIT_SMALL = 4;

    /**
     * The result of one experiment: the steady state before it, what was counted while the failure was in place, the verdict
     * on the control, the audit records the trail dropped or kept waiting, and the monitor's probes of the hurt dependency.
     */
    record Result(Experiment experiment, boolean steadyState, List<Decision> steady, int steadyAudit, int approved, int total,
                  int unscreened, int reviews, int lost, int dropped, int waiting, boolean controlHeld, String failure,
                  List<Integer> probes, boolean alertRaised, boolean alertHeld) {

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
    Map<Experiment, Result> runAll(boolean hardened) throws IOException, InterruptedException {
        Map<Experiment, Result> results = new LinkedHashMap<>();
        for (Experiment e : Experiment.all()) {
            results.put(e, run(e, hardened));
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
            List<Decision> steady = steadyState(service, audit);
            int steadyAudit = backends.auditCount();
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
            int dropped = 0;
            int waiting = 0;
            if (auditScenario) {
                audit.flush(); // let the sender give up on everything it holds while the fault is still in place
                dropped = audit instanceof DroppingAudit d ? d.dropped() : 0;
                waiting = audit instanceof SpooledAudit w ? w.waiting() : 0;
            }
            proxy.reset();
            if (auditScenario) {
                audit.flush();
                obs = auditObservation(e, obs.approved, obs.total);
            }
            return result(e, steady, steadyAudit, obs, dropped, waiting, monitor, hardened);
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
    static final List<Payment> STEADY = List.of(new Payment("s1", "std-1", 120, "ok-1"), new Payment("s2", "std-2", 80, "ok-1"),
            new Payment("s3", "std-3", 9000, "ok-1"), new Payment("s4", "std-4", 120, "rev-7"));

    private List<Decision> steadyState(BankPayments service, AuditTrail audit) {
        List<Decision> decisions = new ArrayList<>();
        for (Payment p : STEADY) {
            decisions.add(service.pay(p));
        }
        audit.flush();
        return decisions;
    }

    /** The steady state holds when the two good payments are approved, the other two refused and every decision audited. */
    static boolean steadyHolds(List<Decision> d, int audited) {
        return d.get(0).approved() && d.get(1).approved() && !d.get(2).approved() && !d.get(3).approved() && audited == STEADY.size();
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

    private Result result(Experiment e, List<Decision> steady, int steadyAudit, Observation o, int dropped, int waiting, Monitor monitor,
                          boolean hardened) {
        boolean held;
        String failure;
        switch (e.scenario()) {
            case AUTHORIZATION -> {
                held = o.approved == 0;
                failure = "fails open";
            }
            case FRAUD -> {
                held = o.unscreened == 0;
                failure = "payments go unscreened";
            }
            case REVOCATION -> {
                held = o.approved == 0;
                failure = "the check soft-fails";
            }
            case RATE_BURST -> {
                held = o.approved <= BankPayments.LIMIT;
                failure = "the limit disappears";
            }
            default -> {
                held = o.lost == 0;
                failure = "audit records silently lost";
            }
        }
        boolean raised = monitor.alerted(e.dependency());
        boolean other = monitor.alerts().stream().anyMatch(a -> !a.dependency().equals(e.dependency()));
        boolean alertHeld;
        if (!hardened) {
            alertHeld = !e.alertExpected();
        } else if (raised) {
            int n = monitor.alerts().stream().filter(a -> a.dependency().equals(e.dependency())).findFirst().orElseThrow().failedProbes();
            alertHeld = e.alertExpected() && !other && n <= Monitor.THRESHOLD;
        } else {
            alertHeld = !e.alertExpected() && !other;
        }
        return new Result(e, steadyHolds(steady, steadyAudit), steady, steadyAudit, o.approved, o.total, o.unscreened, o.reviews, o.lost,
                dropped, waiting, held, failure, monitor.history(e.dependency()), raised, alertHeld);
    }

    /** The one case the hardened version still accepts: a certificate revoked after its last good answer was cached. */
    int staleCacheRisk() throws IOException, InterruptedException {
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
            return approved ? 1 : 0;
        }
    }
}
