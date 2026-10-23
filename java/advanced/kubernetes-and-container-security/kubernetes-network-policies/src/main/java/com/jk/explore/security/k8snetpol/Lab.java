package com.jk.explore.security.k8snetpol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** The bank's five pods on a real cluster, a server outside it, and the measurement of who can reach whom. */
public final class Lab implements AutoCloseable {

    static final Workload WEB = new Workload("shop", "web", "web");
    static final Workload JOBS = new Workload("shop", "jobs", "jobs");
    static final Workload API = new Workload("pay", "api", "api");
    static final Workload DB = new Workload("pay", "db", "db");
    static final Workload ADMIN = new Workload("ops", "admin", "admin");
    static final List<Workload> PODS = List.of(WEB, JOBS, API, DB, ADMIN);
    static final List<String> NAMESPACES = List.of("shop", "pay", "ops");

    /** The design: the shop's web talks to the api, the api to the database, and the operators' admin pod to the database. */
    static final Intent INTENT = new Intent(Set.of(Intent.pair(WEB, API), Intent.pair(API, DB), Intent.pair(ADMIN, DB)));

    final Cluster cluster = new Cluster();
    private final String outsideName = "outside-" + UUID.randomUUID().toString().substring(0, 6);
    private String outsideIp;

    public Lab() throws IOException, InterruptedException {
        cluster.start(PODS);
        cluster.apply("""
                apiVersion: v1
                kind: Service
                metadata: {name: db, namespace: pay}
                spec:
                  selector: {app: db}
                  ports: [{port: 8080}]
                """);
        Sh.must(List.of("docker", "run", "-d", "--name", outsideName, "--network", "kind", "nginx:alpine"));
        outsideIp = Sh.must(List.of("docker", "inspect", "-f", "{{.NetworkSettings.Networks.kind.IPAddress}}", outsideName)).strip();
    }

    /** Applies the manifests and gives the cluster's network plugin a moment to program them. */
    void apply(String... manifests) throws IOException, InterruptedException {
        for (String m : manifests) {
            cluster.apply(m);
        }
        Thread.sleep(6_000);
    }

    void reset() throws IOException, InterruptedException {
        cluster.clearPolicies(NAMESPACES);
        Thread.sleep(6_000);
    }

    /** The design's policies: deny everything in, then one allow for each line of the design. */
    void applyIntended() throws IOException, InterruptedException {
        apply(Policies.denyAllIngress("shop"), Policies.denyAllIngress("pay"), Policies.denyAllIngress("ops"),
                Policies.allowIngress("pay", "api", "shop", "web"),
                Policies.allowIngress("pay", "db", "pay", "api"),
                Policies.allowIngress("pay", "db", "ops", "admin"));
    }

    /** Every pod tries every other pod's address; the pairs that answered. */
    Set<String> measure() throws Exception {
        List<String> ips = new ArrayList<>();
        for (Workload w : PODS) {
            ips.add(cluster.ip(w));
        }
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            List<String> pairs = new ArrayList<>();
            List<Future<Integer>> exits = new ArrayList<>();
            for (Workload from : PODS) {
                for (int i = 0; i < PODS.size(); i++) {
                    Workload to = PODS.get(i);
                    if (!from.equals(to)) {
                        String url = "http://" + ips.get(i) + ":8080/hostname";
                        pairs.add(Intent.pair(from, to));
                        exits.add(pool.submit(() -> cluster.curl(from, url)));
                    }
                }
            }
            Set<String> reachable = new LinkedHashSet<>();
            for (int i = 0; i < pairs.size(); i++) {
                if (exits.get(i).get() == 0) {
                    reachable.add(pairs.get(i));
                }
            }
            return reachable;
        }
    }

    Intent.Diff diff() throws Exception {
        return INTENT.compare(PODS, measure());
    }

    /** Can the pod reach the outside server? Curl's exit code, 0 for yes. */
    int toOutside(Workload from) throws IOException, InterruptedException {
        return cluster.curl(from, "http://" + outsideIp + ":80/");
    }

    /** Can the pod reach the database by its service name? Curl's exit code. */
    int dbByName(Workload from) throws IOException, InterruptedException {
        return cluster.curl(from, "http://db.pay.svc.cluster.local:8080/hostname");
    }

    @Override
    public void close() {
        try {
            Sh.run("docker", "rm", "-f", outsideName);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        cluster.close();
    }
}
