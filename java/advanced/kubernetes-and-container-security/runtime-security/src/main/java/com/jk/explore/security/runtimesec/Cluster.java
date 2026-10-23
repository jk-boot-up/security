package com.jk.explore.security.runtimesec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** A real Kubernetes cluster made by kind (one node, itself a container), with its own kubeconfig file, deleted on close. */
public final class Cluster implements AutoCloseable {

    private final String name = "runtime-" + UUID.randomUUID().toString().substring(0, 6);
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

    void waitReady(String namespace, String pod) throws IOException, InterruptedException {
        must("-n", namespace, "wait", "--for=condition=Ready", "pod/" + pod, "--timeout=180s");
    }

    /** Runs a command in a pod with kubectl exec and no input. */
    Sh.Result exec(String pod, String... command) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of("-n", Pods.NAMESPACE, "exec", pod, "--"));
        args.addAll(List.of(command));
        return kubectl(args.toArray(new String[0]));
    }

    /** Runs a command in a pod with the given text on its standard input, then keeps the input open for holdMillis so a short command is still running when the sensor looks for it. */
    Sh.Result execWithInput(String pod, String input, long holdMillis, String... command) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of("kubectl", "--kubeconfig", kubeconfig.toString(), "-n", Pods.NAMESPACE, "exec", "-i", pod, "--"));
        args.addAll(List.of(command));
        Process p = new ProcessBuilder(args).redirectErrorStream(true).start();
        try (var in = p.getOutputStream()) {
            in.write(input.getBytes(StandardCharsets.UTF_8));
            in.flush();
            Thread.sleep(holdMillis);
        }
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!p.waitFor(120, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IllegalStateException("did not finish: " + args);
        }
        return new Sh.Result(p.exitValue(), out);
    }

    /** Container id to pod name, for the pods of the bank namespace. */
    Map<String, String> containers() throws IOException, InterruptedException {
        String out = must("-n", Pods.NAMESPACE, "get", "pods", "-o",
                "jsonpath={range .items[*]}{.metadata.name} {.status.containerStatuses[0].containerID}{\"\\n\"}{end}");
        Map<String, String> map = new HashMap<>();
        for (String line : out.split("\n")) {
            String[] parts = line.trim().split(" ");
            if (parts.length == 2 && parts[1].startsWith("containerd://")) {
                map.put(parts[1].substring("containerd://".length()), parts[0]);
            }
        }
        return map;
    }

    String serviceIp(String namespace, String service) throws IOException, InterruptedException {
        return must("-n", namespace, "get", "service", service, "-o", "jsonpath={.spec.clusterIP}").trim();
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
