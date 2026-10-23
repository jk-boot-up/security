package com.jk.explore.security.meshmtls;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A real Kubernetes cluster made by kind (one node, itself a container), with its own kubeconfig file, deleted on close. */
public final class Cluster implements AutoCloseable {

    private final String name = "mesh-" + UUID.randomUUID().toString().substring(0, 6);
    private final Path kubeconfig;

    public Cluster() throws IOException, InterruptedException {
        this.kubeconfig = Files.createTempFile("kubeconfig", ".yaml");
        Sh.must(List.of("kind", "create", "cluster", "--name", name, "--kubeconfig", kubeconfig.toString(), "--wait", "180s"));
    }

    /** The container that is the cluster's node. */
    String node() {
        return name + "-control-plane";
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

    /** Applies a manifest through a temporary file that is deleted at once (the Linkerd manifest holds its signing key). */
    void apply(String manifest, String... extra) throws IOException, InterruptedException {
        Path file = Files.createTempFile("manifest", ".yaml");
        try {
            Files.writeString(file, manifest);
            List<String> args = new ArrayList<>(List.of("apply"));
            args.addAll(List.of(extra));
            args.addAll(List.of("-f", file.toString()));
            must(args.toArray(String[]::new));
        } finally {
            Files.delete(file);
        }
    }

    /** Runs the Linkerd CLI against this cluster. */
    Sh.Result linkerd(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(Linkerd.cli().toString()));
        command.addAll(List.of(args));
        command.addAll(List.of("--kubeconfig", kubeconfig.toString()));
        return Sh.run(command);
    }

    String linkerdMust(String... args) throws IOException, InterruptedException {
        Sh.Result r = linkerd(args);
        if (!r.ok()) {
            throw new IllegalStateException("linkerd " + String.join(" ", args) + " failed: " + r.output());
        }
        return r.output();
    }

    /** Installs Linkerd's control plane; the CLI makes the trust anchor and the issuer certificate itself. */
    void installLinkerd(String issuanceLifetime) throws IOException, InterruptedException {
        must("apply", "--server-side", "-f", Linkerd.gatewayApi().toString());
        apply(Sh.mustStdout(List.of(Linkerd.cli().toString(), "install", "--crds", "--kubeconfig", kubeconfig.toString())));
        apply(Sh.mustStdout(List.of(Linkerd.cli().toString(), "install", "--identity-issuance-lifetime=" + issuanceLifetime, "--kubeconfig", kubeconfig.toString())));
        must("-n", "linkerd", "rollout", "status", "deploy", "--timeout=300s");
    }

    /** Calls a URL with curl from inside the workload's main container: the HTTP status, or 0 when nothing answered. */
    int http(Service from, String url) throws IOException, InterruptedException {
        Sh.Result r = kubectl("-n", from.namespace(), "exec", "deploy/" + from.name(), "-c", "main", "--",
                "curl", "-s", "-m", "3", "-o", "/dev/null", "-w", "%{http_code}", url);
        return r.ok() ? Integer.parseInt(r.output().strip()) : 0;
    }

    /** Like http, but returns the body of the answer. */
    String body(Service from, String url) throws IOException, InterruptedException {
        return kubectl("-n", from.namespace(), "exec", "deploy/" + from.name(), "-c", "main", "--", "curl", "-s", "-m", "3", url).output();
    }

    /** The cluster's node calls a pod address itself. */
    int httpFromNode(String url) throws IOException, InterruptedException {
        Sh.Result r = Sh.run("docker", "exec", node(), "curl", "-s", "-m", "3", "-o", "/dev/null", "-w", "%{http_code}", url);
        return r.ok() ? Integer.parseInt(r.output().strip()) : 0;
    }

    String podIp(Service s) throws IOException, InterruptedException {
        return must("-n", s.namespace(), "get", "pod", "-l", "app=" + s.name(), "-o", "jsonpath={.items[0].status.podIP}");
    }

    String podName(Service s) throws IOException, InterruptedException {
        return must("-n", s.namespace(), "get", "pod", "-l", "app=" + s.name(), "-o", "jsonpath={.items[0].metadata.name}");
    }

    /** The process id (on the node) of the pod's sandbox: its network namespace is the one to enter. */
    int podPid(Service s) throws IOException, InterruptedException {
        String pod = podName(s);
        String id = Sh.must(List.of("docker", "exec", node(), "crictl", "pods", "--name", pod, "-q")).strip();
        String info = Sh.must(List.of("docker", "exec", node(), "crictl", "inspectp", id));
        Matcher m = Pattern.compile("\"pid\":\\s*(\\d+)").matcher(info);
        while (m.find()) {
            int pid = Integer.parseInt(m.group(1));
            if (pid > 1) {
                return pid;
            }
        }
        throw new IllegalStateException("no pid for " + pod);
    }

    /** The proxy's own counters for the workload's pod. */
    String proxyMetrics(Service s) throws IOException, InterruptedException {
        return linkerdMust("diagnostics", "proxy-metrics", "-n", s.namespace(), "deploy/" + s.name());
    }

    /** The certificate the workload's proxy holds now, as printed by linkerd identity. */
    CertInfo identity(Service s) throws IOException, InterruptedException {
        IllegalArgumentException last = null;
        for (int i = 0; i < 10; i++) {
            try {
                return CertInfo.fromIdentityOutput(linkerdMust("identity", "-n", s.namespace(), "-l", "app=" + s.name()));
            } catch (IllegalArgumentException e) {
                last = e;
                Thread.sleep(3_000);
            }
        }
        throw last;
    }

    @Override
    public void close() {
        try {
            Sh.run("kind", "delete", "cluster", "--name", name, "--kubeconfig", kubeconfig.toString());
            Files.deleteIfExists(kubeconfig);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
