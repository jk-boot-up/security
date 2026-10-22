package com.jk.explore.security.containerhard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A real Kubernetes cluster made by kind (one node, itself a container), with its own kubeconfig file, deleted on close. */
public final class Cluster implements AutoCloseable {

    private final String name = "hardening-" + UUID.randomUUID().toString().substring(0, 6);
    private final Path kubeconfig;

    public Cluster() throws IOException, InterruptedException {
        this.kubeconfig = Files.createTempFile("kubeconfig", ".yaml");
        Sh.must(List.of("kind", "create", "cluster", "--name", name, "--kubeconfig", kubeconfig.toString(), "--wait", "180s"));
        Sh.must(List.of("kind", "load", "docker-image", Image.TAG, "--name", name));
    }

    /** kubectl against this cluster only. */
    Sh.Result kubectl(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("kubectl", "--kubeconfig", kubeconfig.toString()));
        command.addAll(List.of(args));
        return Sh.run(command);
    }

    /** Applies a manifest (given as text) with kubectl apply; {@code extra} are further kubectl options such as --dry-run=server. */
    Sh.Result apply(String namespace, String manifest, String... extra) throws IOException, InterruptedException {
        Path file = Files.createTempFile("manifest", ".yaml");
        Files.writeString(file, manifest);
        List<String> args = new ArrayList<>(List.of("-n", namespace, "apply", "-f", file.toString()));
        args.addAll(List.of(extra));
        return kubectl(args.toArray(String[]::new));
    }

    /** A namespace that enforces the given Pod Security Standard ("privileged", "baseline" or "restricted") and warns about the given one. */
    void namespace(String name, String enforce, String warn) throws IOException, InterruptedException {
        kubectl("create", "namespace", name);
        kubectl("label", "namespace", name, "pod-security.kubernetes.io/enforce=" + enforce, "pod-security.kubernetes.io/warn=" + warn, "--overwrite");
    }

    /** The thirteen attempts, run inside a running pod as the user the pod runs as. */
    public Map<String, Boolean> probe(String namespace, String pod) throws IOException, InterruptedException {
        Sh.must(List.of("kubectl", "--kubeconfig", kubeconfig.toString(), "-n", namespace, "wait", "--for=condition=Ready", "pod/" + pod, "--timeout=120s"));
        return Probes.parse(kubectl("-n", namespace, "exec", pod, "--", "sh", "-c", Probes.SCRIPT).output());
    }

    String serverVersion() throws IOException, InterruptedException {
        Matcher m = Pattern.compile("\"gitVersion\":\\s*\"(v[0-9.]+)\"").matcher(kubectl("version", "-o", "json").output());
        String last = "";
        while (m.find()) {
            last = m.group(1);
        }
        return last;
    }

    /** The names of the Pod Security Standard checks that a refusal lists, such as "host namespaces" and "privileged", in the order the API server gives them. */
    static List<String> violations(String message) {
        int at = message.indexOf("violates PodSecurity \"");
        if (at < 0) {
            at = message.indexOf("would violate PodSecurity \"");
        }
        if (at < 0) {
            return List.of();
        }
        String list = message.substring(message.indexOf("\": ", at) + 3);
        List<String> out = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : list.strip().toCharArray()) {
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            if (c == ',' && depth == 0) {
                out.add(name(current.toString()));
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        out.add(name(current.toString()));
        return out;
    }

    private static String name(String violation) {
        String v = violation.strip();
        int paren = v.indexOf(" (");
        return paren < 0 ? v : v.substring(0, paren);
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
