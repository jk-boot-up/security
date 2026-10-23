package com.jk.explore.security.runtimesec;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The bank's cluster, an outside server and the sensor, and what the sensor saw. */
public final class Lab implements AutoCloseable {

    final Cluster cluster;
    final Sensor sensor;
    private final String outsideName = "runtimesec-outside-" + UUID.randomUUID().toString().substring(0, 6);
    final String outsideIp;
    final Rules.Baseline baseline;
    private final Set<Alert> reported = new HashSet<>();
    private long mark;

    public Lab() throws IOException, InterruptedException {
        Cluster c = null;
        Sensor s = null;
        try {
            c = new Cluster();
            s = new Sensor();
            Sh.must(List.of("docker", "run", "-d", "--name", outsideName, "--network", "kind", "busybox:1.37", "httpd", "-f", "-p", "4444", "-h", "/tmp"));
            outsideIp = Sh.must(List.of("docker", "inspect", "-f", "{{.NetworkSettings.Networks.kind.IPAddress}}", outsideName)).trim();
            c.must("create", "namespace", Pods.NAMESPACE);
            c.apply(Pods.ledger());
            c.apply(Pods.api(Pods.API, false));
            c.apply(Pods.api(Pods.API_RO, true));
            for (String pod : List.of(Pods.LEDGER, Pods.API, Pods.API_RO)) {
                c.waitReady(Pods.NAMESPACE, pod);
            }
            String dns = c.must("-n", "kube-system", "get", "service", "kube-dns", "-o", "jsonpath={.spec.clusterIP}").trim();
            baseline = new Rules.Baseline(List.of("/var/log/payments/", "/tmp/", "/dev/"), Set.of(c.serviceIp(Pods.NAMESPACE, Pods.LEDGER), dns));
        } catch (IOException | InterruptedException | RuntimeException e) {
            Sh.run("docker", "rm", "-f", outsideName);
            if (s != null) {
                s.close();
            }
            if (c != null) {
                c.close();
            }
            throw e;
        }
        this.cluster = c;
        this.sensor = s;
        // The start of the pods is not the service's behaviour: only what happens after this point is judged.
        Thread.sleep(5_000);
        this.mark = events().stream().mapToLong(e -> Long.parseLong(e.id())).max().orElse(0);
    }

    /** Waits for the sensor to copy the records of what just ran. */
    private void settle() throws InterruptedException {
        Thread.sleep(4_000);
    }

    private List<Event> events() throws IOException, InterruptedException {
        settle();
        return Audit.parse(sensor.log()).stream().filter(e -> Long.parseLong(e.id()) > mark).toList();
    }

    /** The alerts raised since the last call. */
    Set<Alert> newAlerts() throws IOException, InterruptedException {
        Map<String, String> pods = cluster.containers();
        Set<Alert> all = Rules.evaluate(events(), pods, baseline);
        all.removeAll(reported);
        reported.addAll(all);
        return all;
    }

    /** The kinds of syscall the sensor has seen from a pod (exec, write, connect). */
    Set<String> seenFrom(String pod) throws IOException, InterruptedException {
        Map<String, String> pods = cluster.containers();
        Set<String> kinds = new HashSet<>();
        for (Event e : events()) {
            if (e.containerId() != null && pod.equals(pods.get(e.containerId()))) {
                kinds.add(e.key());
            }
        }
        return kinds;
    }

    @Override
    public void close() {
        try {
            Sh.run("docker", "rm", "-f", outsideName);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        sensor.close();
        cluster.close();
    }
}
