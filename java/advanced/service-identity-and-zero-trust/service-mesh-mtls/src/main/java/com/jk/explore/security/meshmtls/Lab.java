package com.jk.explore.security.meshmtls;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The bank's workloads on a real cluster with Linkerd installed, and the measurements of who can call whom. */
public final class Lab implements AutoCloseable {

    static final Service WEB = new Service("bank", "web");
    static final Service PAYMENTS = new Service("bank", "payments");
    static final Service LEDGER = new Service("bank", "ledger");
    static final Service STATEMENTS = new Service("bank", "statements");
    /** A batch job of an older system, in a namespace that is never injected. */
    static final Service BATCH = new Service("legacy", "batch");
    static final List<Service> BANK = List.of(WEB, PAYMENTS, LEDGER, STATEMENTS);
    static final List<Service> CALLERS = List.of(WEB, PAYMENTS, LEDGER, STATEMENTS, BATCH);

    /** The design: the web front calls payments, and payments calls the ledger. Nothing else. */
    static final Design DESIGN = new Design(Set.of(Design.pair(WEB, PAYMENTS), Design.pair(PAYMENTS, LEDGER)));

    /** Linkerd's default is 24 hours; a short one lets the demo watch a renewal. */
    static final String ISSUANCE_LIFETIME = "5m0s";

    final Cluster cluster = new Cluster();
    final Sniffer sniffer;

    public Lab() throws IOException, InterruptedException {
        Sniffer s;
        try {
            cluster.installLinkerd(ISSUANCE_LIFETIME);
            for (String ns : List.of("bank", "legacy", "shadow")) {
                cluster.must("create", "namespace", ns);
            }
            for (Service svc : CALLERS) {
                cluster.apply(svc.manifest());
            }
            settle("bank", 4);
            settle("legacy", 1);
            s = new Sniffer(cluster.node());
        } catch (IOException | InterruptedException | RuntimeException e) {
            cluster.close();
            throw e;
        }
        sniffer = s;
    }

    /** Waits until the namespace has exactly this many pods, all ready (the old pods of a restart are gone). */
    void settle(String namespace, int pods) throws IOException, InterruptedException {
        for (int i = 0; i < 120; i++) {
            String names = cluster.must("-n", namespace, "get", "pods", "-o", "name").strip();
            int count = names.isEmpty() ? 0 : names.split("\n").length;
            if (count == pods) {
                break;
            }
            Thread.sleep(1_000);
        }
        cluster.must("-n", namespace, "wait", "--for=condition=Ready", "pod", "--all", "--timeout=180s");
    }

    /** Turns injection on for the namespace and restarts its pods so each one gets a proxy. */
    void inject(String namespace, int pods) throws IOException, InterruptedException {
        cluster.must("annotate", "namespace", namespace, "linkerd.io/inject=enabled", "--overwrite");
        cluster.must("-n", namespace, "rollout", "restart", "deploy");
        cluster.must("-n", namespace, "rollout", "status", "deploy", "--timeout=240s");
        settle(namespace, pods);
    }

    /** How many pods of the two namespaces run a Linkerd proxy. */
    int proxies() throws IOException, InterruptedException {
        int n = 0;
        for (String ns : List.of("bank", "legacy")) {
            String lines = cluster.must("-n", ns, "get", "pods", "-o",
                    "jsonpath={range .items[*]}{.spec.containers[*].name} {.spec.initContainers[*].name}{\"\\n\"}{end}");
            for (String line : lines.split("\n")) {
                if (line.contains("linkerd-proxy")) {
                    n++;
                }
            }
        }
        return n;
    }

    /** Every caller calls every bank service; the pairs that answered with 200. */
    Set<String> measure() throws IOException, InterruptedException {
        Set<String> answered = new LinkedHashSet<>();
        for (Service from : CALLERS) {
            for (Service to : BANK) {
                if (!from.equals(to) && cluster.http(from, to.url("/hostname")) == 200) {
                    answered.add(Design.pair(from, to));
                }
            }
        }
        return answered;
    }

    Design.Diff diff() throws IOException, InterruptedException {
        return DESIGN.compare(CALLERS, BANK, measure());
    }

    /** Applies the policy of the design: every bank service has a Server, and only the named callers are let in. */
    void applyDesignPolicy() throws IOException, InterruptedException {
        cluster.apply(Policies.forService(WEB, List.of()));
        cluster.apply(Policies.forService(STATEMENTS, List.of()));
        cluster.apply(Policies.forService(PAYMENTS, List.of(WEB)));
        cluster.apply(Policies.forService(LEDGER, List.of(PAYMENTS)));
        Thread.sleep(8_000);
    }

    int restarts(Service s) throws IOException, InterruptedException {
        String counts = cluster.must("-n", s.namespace(), "get", "pod", "-l", "app=" + s.name(), "-o",
                "jsonpath={.items[0].status.containerStatuses[*].restartCount}");
        int sum = 0;
        for (String c : counts.strip().split("\\s+")) {
            sum += Integer.parseInt(c);
        }
        return sum;
    }

    @Override
    public void close() {
        sniffer.close();
        cluster.close();
    }
}
