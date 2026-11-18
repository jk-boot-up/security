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

/** The online store's five pods on a real cluster, a server outside it, and the measurement of who can reach whom. */
public final class Lab implements AutoCloseable {

    static final Workload WEB = new Workload("shop", "web", "web", "the store front");
    static final Workload JOBS = new Workload("shop", "jobs", "jobs", "the nightly batch jobs");
    static final Workload API = new Workload("pay", "api", "api", "the payments api");
    static final Workload DB = new Workload("pay", "db", "db", "the payments database");
    static final Workload ADMIN = new Workload("ops", "admin", "admin", "the operators' console");
    static final List<Workload> PODS = List.of(WEB, JOBS, API, DB, ADMIN);
    static final List<String> NAMESPACES = List.of("shop", "pay", "ops");

    /** The design: the store front talks to the api, the api to the database, and the operators' console to the database. */
    static final Intent INTENT = new Intent(Set.of(Intent.pair(WEB, API), Intent.pair(API, DB), Intent.pair(ADMIN, DB)));

    /** A default deny of ingress in each namespace. */
    static final List<Policy> DENY_ALL = NAMESPACES.stream().map(Policies::denyAllIngress).toList();

    /** One allow for each line of the design. */
    static final List<Policy> ALLOWS = List.of(
            Policies.allowIngress("pay", "api", "shop", "web"),
            Policies.allowIngress("pay", "db", "pay", "api"),
            Policies.allowIngress("pay", "db", "ops", "admin"));

    /** The server outside the cluster: the same pinned image, as a plain container on kind's Docker network. */
    private static final String OUTSIDE_IMAGE = Workload.IMAGE;

    final Cluster cluster = new Cluster();
    private final String outsideName = "outside-" + UUID.randomUUID().toString().substring(0, 6);
    private final String outsideIp;

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
        Sh.must(List.of("docker", "run", "-d", "--name", outsideName, "--network", "kind", OUTSIDE_IMAGE, "netexec", "--http-port=80"));
        outsideIp = Sh.must(List.of("docker", "inspect", "-f", "{{.NetworkSettings.Networks.kind.IPAddress}}", outsideName)).strip();
    }

    /** Applies the policies, gives the cluster's network plugin a moment to program them, and returns the API server's answers. */
    List<String> apply(List<Policy> policies) throws IOException, InterruptedException {
        List<String> answers = new ArrayList<>();
        for (Policy p : policies) {
            answers.add(cluster.apply(p.manifest()));
        }
        Thread.sleep(6_000);
        return answers;
    }

    List<String> apply(Policy... policies) throws IOException, InterruptedException {
        return apply(List.of(policies));
    }

    void reset() throws IOException, InterruptedException {
        cluster.clearPolicies(NAMESPACES);
        Thread.sleep(6_000);
    }

    /** The design's policies: deny everything in, then one allow for each line of the design. */
    void applyIntended() throws IOException, InterruptedException {
        List<Policy> all = new ArrayList<>(DENY_ALL);
        all.addAll(ALLOWS);
        apply(all);
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

    /** Can the pod reach the outside server? Curl's exit code, 0 for yes. */
    int toOutside(Workload from) throws IOException, InterruptedException {
        return cluster.curl(from, "http://" + outsideIp + ":80/hostname");
    }

    /** Can the pod reach the database by its service name (a DNS lookup first)? Curl's exit code. */
    int dbByName(Workload from) throws IOException, InterruptedException {
        return cluster.curl(from, "http://db.pay.svc.cluster.local:8080/hostname");
    }

    /** Can the pod reach the database by its address (no lookup)? Curl's exit code. */
    int dbByAddress(Workload from) throws IOException, InterruptedException {
        return cluster.curl(from, "http://" + cluster.ip(DB) + ":8080/hostname");
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
