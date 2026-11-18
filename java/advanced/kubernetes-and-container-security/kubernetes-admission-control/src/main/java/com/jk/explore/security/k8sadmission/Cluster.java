package com.jk.explore.security.k8sadmission;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A real Kubernetes cluster made by kind (one node, itself a container), with its own kubeconfig file, deleted on close. */
public final class Cluster implements AutoCloseable {

    /** The node image (Kubernetes 1.37.0) pinned by digest, so a new kind release cannot change the cluster under the demo. */
    static final String NODE_IMAGE = "kindest/node:v1.37.0@sha256:a1ed56cfb0e7b93589bdf97c8cd566405a265939e3620fc4f5de89adff580ae5";

    private final String name = "admission-" + UUID.randomUUID().toString().substring(0, 6);
    private final Path kubeconfig;

    public Cluster() throws IOException, InterruptedException {
        this.kubeconfig = Files.createTempFile("kubeconfig", ".yaml");
        Sh.must(List.of("kind", "create", "cluster", "--name", name, "--image", NODE_IMAGE, "--kubeconfig", kubeconfig.toString(), "--wait", "180s"));
    }

    /** kubectl against this cluster only. */
    Sh.Result kubectl(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("kubectl", "--kubeconfig", kubeconfig.toString()));
        command.addAll(List.of(args));
        for (int attempt = 1; ; attempt++) {
            Sh.Result r = Sh.run(command);
            if (r.ok() || attempt == 5 || !isTransient(r.output())) {
                return r;
            }
            Thread.sleep(10_000);
        }
    }

    /** A timeout on a busy machine (a webhook's first call, the API server's TLS handshake, a namespace whose
     *  default service account is not made yet) is tried again; a refusal is not. */
    static boolean isTransient(String output) {
        return output.contains("context deadline exceeded") || output.contains("TLS handshake timeout") || output.contains("client connection lost")
                || output.contains("serviceaccount \"default\" not found");
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
