package com.jk.explore.security.k8sadmission;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A real Kubernetes cluster made by kind (one node, itself a container), with its own kubeconfig file, deleted on close. */
public final class Cluster implements AutoCloseable {

    private final String name = "admission-" + UUID.randomUUID().toString().substring(0, 6);
    private final Path kubeconfig;

    public Cluster() throws IOException, InterruptedException {
        this.kubeconfig = Files.createTempFile("kubeconfig", ".yaml");
        Sh.must(List.of("kind", "create", "cluster", "--name", name, "--kubeconfig", kubeconfig.toString(), "--wait", "180s"));
    }

    /** kubectl against this cluster only. */
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

    /** Applies a manifest given as text. {@code extra} are further kubectl options such as --dry-run=server. */
    Sh.Result apply(String namespace, String manifest, String... extra) throws IOException, InterruptedException {
        Path file = Files.createTempFile("manifest", ".yaml");
        Files.writeString(file, manifest);
        List<String> args = new ArrayList<>(List.of("-n", namespace, "apply", "-f", file.toString()));
        args.addAll(List.of(extra));
        return kubectl(args.toArray(String[]::new));
    }

    /** Creates a namespace; when {@code enforced}, the bank's admission policies apply to it. */
    void namespace(String name, boolean enforced) throws IOException, InterruptedException {
        must("create", "namespace", name);
        if (enforced) {
            must("label", "namespace", name, "bank/admission=enforced");
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
