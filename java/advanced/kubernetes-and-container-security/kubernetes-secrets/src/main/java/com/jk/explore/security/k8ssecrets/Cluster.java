package com.jk.explore.security.k8ssecrets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A real Kubernetes cluster made by kind (one node, itself a container), with its own kubeconfig file, deleted on close.
 * Its API server reads an EncryptionConfiguration from a folder shared with this program, which starts with no encryption.
 */
public final class Cluster implements AutoCloseable {

    private static final String ENC_DIR = "/etc/kubernetes/enc";
    private static final String APISERVER = "/etc/kubernetes/manifests/kube-apiserver.yaml";

    private final String name = "secrets-" + UUID.randomUUID().toString().substring(0, 6);
    private final Path kubeconfig;
    private final Path encDir;
    private final String key = Encryption.newKey();

    public Cluster() throws IOException, InterruptedException {
        this.kubeconfig = Files.createTempFile("kubeconfig", ".yaml");
        this.encDir = Files.createTempDirectory(Files.createDirectories(Path.of("build")).toAbsolutePath(), "enc");
        Files.writeString(encDir.resolve("enc.yaml"), Encryption.identityOnly());
        Path config = Files.createTempFile("kind", ".yaml");
        Files.writeString(config, """
                kind: Cluster
                apiVersion: kind.x-k8s.io/v1alpha4
                nodes:
                  - role: control-plane
                    extraMounts:
                      - hostPath: %s
                        containerPath: %s
                kubeadmConfigPatches:
                  - |
                    kind: ClusterConfiguration
                    apiServer:
                      extraArgs:
                        encryption-provider-config: %s/enc.yaml
                      extraVolumes:
                        - name: enc
                          hostPath: %s
                          mountPath: %s
                          readOnly: true
                          pathType: Directory
                """.formatted(encDir, ENC_DIR, ENC_DIR, ENC_DIR, ENC_DIR));
        Sh.must(List.of("kind", "create", "cluster", "--name", name, "--kubeconfig", kubeconfig.toString(), "--config", config.toString(), "--wait", "180s"));
    }

    String node() {
        return name + "-control-plane";
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

    Sh.Result apply(String manifest) throws IOException, InterruptedException {
        Path file = Files.createTempFile("manifest", ".yaml");
        Files.writeString(file, manifest);
        return kubectl("apply", "-f", file.toString());
    }

    /** Creates something from a manifest given as text (a TokenReview cannot be applied, only created). */
    Sh.Result create(String manifest, String... extra) throws IOException, InterruptedException {
        Path file = Files.createTempFile("manifest", ".yaml");
        Files.writeString(file, manifest);
        List<String> args = new ArrayList<>(List.of("create", "-f", file.toString()));
        args.addAll(List.of(extra));
        return kubectl(args.toArray(String[]::new));
    }

    /** Runs a command in a pod. */
    Sh.Result exec(String namespace, String pod, String... command) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of("-n", namespace, "exec", pod, "--"));
        args.addAll(List.of(command));
        return kubectl(args.toArray(String[]::new));
    }

    private List<String> etcdctl(String... args) {
        List<String> command = new ArrayList<>(List.of("--kubeconfig", kubeconfig.toString(), "-n", "kube-system", "exec", "etcd-" + node(), "--",
                "etcdctl", "--endpoints=https://127.0.0.1:2379", "--cacert=/etc/kubernetes/pki/etcd/ca.crt", "--cert=/etc/kubernetes/pki/etcd/server.crt", "--key=/etc/kubernetes/pki/etcd/server.key"));
        command.addAll(List.of(args));
        return command;
    }

    private String etcd(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("kubectl"));
        command.addAll(etcdctl(args));
        return Sh.must(command);
    }

    /** The value etcd holds for a key, as the API server wrote it. */
    Stored etcdValue(String key) throws IOException, InterruptedException {
        return new Stored(etcd("get", key, "--print-value-only"));
    }

    /** The keys etcd holds under a prefix. */
    List<String> etcdKeys(String prefix) throws IOException, InterruptedException {
        return etcd("get", prefix, "--prefix", "--keys-only").lines().filter(l -> !l.isBlank()).toList();
    }

    /** Every value in etcd, one blob. */
    Stored etcdEverything() throws IOException, InterruptedException {
        return new Stored(etcd("get", "", "--from-key", "--print-value-only"));
    }

    /** Writes the aescbc configuration to the shared folder and restarts the API server so that it reads it. */
    void enableEncryption() throws IOException, InterruptedException {
        Files.writeString(encDir.resolve("enc.yaml"), Encryption.aescbc(key));
        Sh.must(List.of("docker", "exec", node(), "sh", "-c", "mv " + APISERVER + " /tmp/apiserver.off && sleep 20 && mv /tmp/apiserver.off " + APISERVER));
        waitReady();
    }

    /** True when the key file is on the control-plane node, where anyone who is root there can read it. */
    boolean keyIsOnTheNode() throws IOException, InterruptedException {
        return Sh.run("docker", "exec", node(), "cat", ENC_DIR + "/enc.yaml").output().contains(key);
    }

    void waitReady() throws IOException, InterruptedException {
        for (int i = 0; i < 90; i++) {
            if (kubectl("get", "--raw", "/readyz").ok()) {
                return;
            }
            Thread.sleep(2000);
        }
        throw new IllegalStateException("the API server did not come back");
    }

    @Override
    public void close() {
        try {
            Sh.run("kind", "delete", "cluster", "--name", name, "--kubeconfig", kubeconfig.toString());
            Files.deleteIfExists(encDir.resolve("enc.yaml"));
            Files.deleteIfExists(encDir);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
