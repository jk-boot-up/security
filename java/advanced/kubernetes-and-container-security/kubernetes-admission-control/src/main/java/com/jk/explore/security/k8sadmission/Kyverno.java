package com.jk.explore.security.k8sadmission;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import tools.jackson.databind.json.JsonMapper;

/** Kyverno, an admission controller, installed from its release manifest, and the bank's policies applied from the policies/ folder. */
public final class Kyverno {

    static final String VERSION = "v1.19.2";
    static final Path POLICIES = Path.of("policies");

    private final Cluster cluster;

    public Kyverno(Cluster cluster) throws IOException, InterruptedException {
        this.cluster = cluster;
        Path manifest = Path.of("build/kyverno-" + VERSION + ".yaml");
        if (!Files.exists(manifest)) {
            Files.createDirectories(manifest.getParent());
            HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
            http.send(HttpRequest.newBuilder(URI.create("https://github.com/kyverno/kyverno/releases/download/" + VERSION + "/install.yaml")).timeout(Duration.ofMinutes(5)).build(),
                    HttpResponse.BodyHandlers.ofFile(manifest));
        }
        cluster.must("apply", "--server-side", "-f", manifest.toAbsolutePath().toString());
        waitForControllers();
        // exceptions are off by default; when on, they are read only from the kyverno namespace, which developers cannot write to
        cluster.must("-n", "kyverno", "patch", "deployment", "kyverno-admission-controller", "--type=json", "-p",
                "[{\"op\":\"add\",\"path\":\"/spec/template/spec/containers/0/args/-\",\"value\":\"--enablePolicyException=true\"},"
                        + "{\"op\":\"add\",\"path\":\"/spec/template/spec/containers/0/args/-\",\"value\":\"--exceptionNamespace=kyverno\"}]");
        waitForControllers();
    }

    void waitForControllers() throws IOException, InterruptedException {
        cluster.must("-n", "kyverno", "rollout", "status", "deployment/kyverno-admission-controller", "--timeout=300s");
        cluster.must("-n", "kyverno", "wait", "--for=condition=Available", "deployment", "--all", "--timeout=300s");
    }

    /** Applies the named policy files (without .yaml) and waits until each one is ready. */
    public void apply(Registry registry, String... names) throws IOException, InterruptedException {
        for (String name : names) {
            String text = Files.readString(POLICIES.resolve(name + ".yaml")).replace("__REGISTRY__", registry.clusterHost()).replace("__PUBLIC_KEY__", indent(registry.publicKey(), 12));
            applyWithRetry(write(text));
        }
        for (String name : names) {
            if (!name.equals("exception")) {
                waitReady(kindOf(name), policyName(name));
            }
        }
        Thread.sleep(10_000);
    }

    /** The policy webhook of a controller that has just restarted can need a moment before it answers. */
    private void applyWithRetry(String file) throws IOException, InterruptedException {
        for (int i = 0; ; i++) {
            Sh.Result r = cluster.kubectl("apply", "-f", file);
            if (r.ok()) {
                return;
            }
            if (i == 5 || !r.output().contains("failed calling webhook")) {
                throw new IllegalStateException("kubectl apply failed: " + r.output());
            }
            Thread.sleep(10_000);
        }
    }

    static String kindOf(String file) {
        return switch (file) {
            case "signed-images" -> "imagevalidatingpolicy";
            case "default-no-escalation" -> "mutatingpolicy";
            default -> "validatingpolicy";
        };
    }

    static String policyName(String file) {
        return switch (file) {
            case "signed-images" -> "signed-images-only";
            case "default-no-escalation" -> "default-no-privilege-escalation";
            default -> file;
        };
    }

    private void waitReady(String kind, String name) throws IOException, InterruptedException {
        for (int i = 0; i < 90; i++) {
            if (cluster.kubectl("get", kind, name, "-o", "jsonpath={.status.conditionStatus.ready}").output().equals("true")) {
                return;
            }
            Thread.sleep(1000);
        }
        throw new IllegalStateException(name + " did not become ready");
    }

    /** Applies the exception, valid until the given instant, in the given namespace. */
    public Sh.Result applyException(String namespace, String expiresAt) throws IOException, InterruptedException {
        String text = Files.readString(POLICIES.resolve("exception.yaml")).replace("__EXPIRES__", expiresAt).replace("namespace: kyverno", "namespace: " + namespace);
        Sh.Result r = cluster.kubectl("apply", "-f", write(text));
        Thread.sleep(8_000);
        return r;
    }

    public void setAction(String policy, String action) throws IOException, InterruptedException {
        cluster.must("patch", "validatingpolicy", policy, "--type=merge", "-p", "{\"spec\":{\"validationActions\":[\"" + action + "\"]}}");
        Thread.sleep(10_000);
    }

    /** Stops or starts the admission controller. */
    public void scale(int replicas) throws IOException, InterruptedException {
        cluster.must("-n", "kyverno", "scale", "deployment/kyverno-admission-controller", "--replicas=" + replicas);
        if (replicas > 0) {
            waitForControllers();
            Thread.sleep(10_000);
        } else {
            cluster.must("-n", "kyverno", "wait", "--for=delete", "pod", "-l", "app.kubernetes.io/component=admission-controller", "--timeout=120s");
        }
    }

    /** The result of a policy report entry: policy name, resource name and result (pass, fail, ...). */
    public record Entry(String policy, String resource, String result) {
    }

    /** Waits until the reports show a result for the policy and resource, then returns all entries of the namespace. */
    public List<Entry> reports(String namespace, String policy, String resource) throws IOException, InterruptedException {
        var json = new JsonMapper();
        for (int i = 0; i < 120; i++) {
            List<Entry> entries = new java.util.ArrayList<>();
            for (var report : json.readTree(cluster.must("-n", namespace, "get", "policyreports.wgpolicyk8s.io", "-o", "json")).get("items")) {
                for (var r : report.path("results")) {
                    String res = report.path("scope").path("name").asString("");
                    entries.add(new Entry(r.path("policy").asString(), res, r.path("result").asString()));
                }
            }
            if (entries.stream().anyMatch(e -> e.policy().equals(policy) && e.resource().equals(resource))) {
                return entries;
            }
            Thread.sleep(2000);
        }
        return List.of();
    }

    private static String indent(String text, int n) {
        String pad = " ".repeat(n);
        return text.strip().lines().map(l -> pad + l).collect(Collectors.joining("\n"));
    }

    private static String write(String text) throws IOException {
        Path file = Files.createTempFile("policy", ".yaml");
        Files.writeString(file, text);
        return file.toString();
    }

    static Map<String, String> none() {
        return Map.of();
    }
}
