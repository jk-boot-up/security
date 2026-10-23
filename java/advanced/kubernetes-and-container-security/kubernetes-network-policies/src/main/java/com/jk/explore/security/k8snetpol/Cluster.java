package com.jk.explore.security.k8snetpol;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A real Kubernetes cluster made by kind (one node, itself a container), with its own kubeconfig file, deleted on close. */
public final class Cluster implements AutoCloseable {

    private final String name = "netpol-" + UUID.randomUUID().toString().substring(0, 6);
    private final Path kubeconfig;

    public Cluster() throws IOException, InterruptedException {
        this.kubeconfig = Files.createTempFile("kubeconfig", ".yaml");
        Sh.must(List.of("kind", "create", "cluster", "--name", name, "--kubeconfig", kubeconfig.toString(), "--wait", "180s"));
    }

    Sh.Result kubectl(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("kubectl", "--kubeconfig", kubeconfig.toString()));
        command.addAll(List.of(args));
        return Sh.run(command);
    }

    String must(String... args) throws IOException, InterruptedException {
        Sh.Result r = kubectl(args);
        if (!r.ok()) {
            throw new IllegalStateException("kubectl " + String.join(" ", args) + " failed: " + r.output());
        }
        return r.output();
    }

    void apply(String manifest) throws IOException, InterruptedException {
        Path file = Files.createTempFile("manifest", ".yaml");
        Files.writeString(file, manifest);
        must("apply", "-f", file.toString());
    }

    /** Starts the pods and waits until each one answers. */
    void start(List<Workload> pods) throws IOException, InterruptedException {
        for (String ns : pods.stream().map(Workload::namespace).distinct().toList()) {
            must("create", "namespace", ns);
        }
        for (Workload w : pods) {
            apply(w.manifest());
        }
        for (Workload w : pods) {
            must("-n", w.namespace(), "wait", "--for=condition=Ready", "pod/" + w.name(), "--timeout=180s");
        }
    }

    String ip(Workload w) throws IOException, InterruptedException {
        return must("-n", w.namespace(), "get", "pod", w.name(), "-o", "jsonpath={.status.podIP}");
    }

    /** Runs curl inside a pod against a URL and returns curl's exit code: 0 answered, 28 timed out, 6 name not found. */
    int curl(Workload from, String url) throws IOException, InterruptedException {
        return kubectl("-n", from.namespace(), "exec", from.name(), "--", "curl", "-s", "-m", "2", "-o", "/dev/null", url).exit();
    }

    /** Removes every network policy of the namespaces. */
    void clearPolicies(List<String> namespaces) throws IOException, InterruptedException {
        for (String ns : namespaces) {
            must("-n", ns, "delete", "networkpolicy", "--all");
        }
    }

    @Override
    public void close() {
        try {
            Sh.run("kind", "delete", "cluster", "--name", name, "--kubeconfig", kubeconfig.toString());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
