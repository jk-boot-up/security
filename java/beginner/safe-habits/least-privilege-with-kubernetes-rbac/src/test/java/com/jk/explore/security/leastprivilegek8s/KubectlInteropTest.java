package com.jk.explore.security.leastprivilegek8s;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

/**
 * Independent check: a real Kubernetes cluster (kind, in Docker) is given the same manifests, and `kubectl auth can-i` must answer
 * exactly as the Java evaluator does. The cluster is always deleted. Run with ./gradlew integrationTest.
 */
@Tag("k8s")
class KubectlInteropTest {

    static final String CLUSTER = "lp-rbac-" + UUID.randomUUID().toString().substring(0, 8);
    static Path kubeconfig;
    static boolean up;

    record Result(int exit, String output) {
    }

    static Result run(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectInput(ProcessBuilder.Redirect.from(Path.of("/dev/null").toFile())).start();
        String output = new String(process.getInputStream().readAllBytes());
        if (!process.waitFor(300, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("timed out: " + String.join(" ", command));
        }
        return new Result(process.exitValue(), output.strip());
    }

    static boolean available(String tool, String... args) {
        try {
            List<String> command = new ArrayList<>(List.of(tool));
            command.addAll(List.of(args));
            return run(command.toArray(String[]::new)).exit() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @BeforeAll
    static void createCluster() throws Exception {
        Assumptions.assumeTrue(available("docker", "info") && available("kind", "version") && available("kubectl", "version", "--client"), "docker, kind and kubectl are needed");
        kubeconfig = Files.createTempFile("kubeconfig", ".yaml");
        up = true;
        Result created = run("kind", "create", "cluster", "--name", CLUSTER, "--kubeconfig", kubeconfig.toString(), "--wait", "120s");
        assertEquals(0, created.exit(), created.output());
        Result applied = run("kubectl", "--kubeconfig", kubeconfig.toString(), "apply", "-f", "infra/rbac/shop.yaml");
        assertEquals(0, applied.exit(), applied.output());
    }

    @AfterAll
    static void deleteCluster() throws Exception {
        if (up) {
            run("kind", "delete", "cluster", "--name", CLUSTER, "--kubeconfig", kubeconfig.toString());
        }
        if (kubeconfig != null) {
            Files.deleteIfExists(kubeconfig);
        }
    }

    private static boolean kubectlSays(String name, String verb, String resource, String namespace) throws Exception {
        Result result = run("kubectl", "--kubeconfig", kubeconfig.toString(), "auth", "can-i", verb, resource, "-n", namespace, "--as=" + LeastPrivilegeK8sDemo.user(name));
        return answer(result).equals("yes");
    }

    /** kubectl may print warnings (an old client, an unusual verb) before the answer, so the answer is the last line. */
    private static String answer(Result result) {
        String[] lines = result.output().split("\n");
        return lines[lines.length - 1].strip();
    }

    @Test
    void theClusterAnswersLikeTheJavaEvaluator() throws Exception {
        Rbac rbac = new Rbac(LeastPrivilegeK8sDemo.load());
        List<String> mismatches = new ArrayList<>();
        for (String name : List.of("nightly-export-admin", "exporter", "reporter", "ci-deployer", "support-agent")) {
            for (String namespace : List.of("shop", "warehouse")) {
                for (String resource : List.of("pods", "secrets", "configmaps", "services", "deployments", "roles", "rolebindings")) {
                    for (String verb : Rbac.VERBS) {
                        boolean java = rbac.can(LeastPrivilegeK8sDemo.user(name), verb, resource, namespace);
                        if (java != kubectlSays(name, verb, resource, namespace)) {
                            mismatches.add(name + " " + verb + " " + resource + " in " + namespace);
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), mismatches);
    }

    @Test
    void theBindEscalateAndExecVerbsAgree() throws Exception {
        Rbac rbac = new Rbac(LeastPrivilegeK8sDemo.load());
        for (String name : List.of("ci-deployer", "exporter", "nightly-export-admin")) {
            for (String verb : List.of("bind", "escalate")) {
                assertEquals(rbac.can(LeastPrivilegeK8sDemo.user(name), verb, "roles", "shop"), kubectlSays(name, verb, "roles.rbac.authorization.k8s.io", "shop"), name + " " + verb);
            }
            // "pods/exec" on the command line would mean a pod named exec, so the subresource is given as a flag
            Result exec = run("kubectl", "--kubeconfig", kubeconfig.toString(), "auth", "can-i", "create", "pods", "--subresource=exec", "-n", "shop", "--as=" + LeastPrivilegeK8sDemo.user(name));
            assertEquals(rbac.can(LeastPrivilegeK8sDemo.user(name), "create", "pods/exec", "shop"), answer(exec).equals("yes"), name + " exec");
        }
    }

    @Test
    void aNamedConfigMapIsReadableAndAnotherIsNot() throws Exception {
        String exporter = "--as=" + LeastPrivilegeK8sDemo.user("exporter");
        Result named = run("kubectl", "--kubeconfig", kubeconfig.toString(), "auth", "can-i", "get", "configmaps/export-settings", "-n", "shop", exporter);
        Result other = run("kubectl", "--kubeconfig", kubeconfig.toString(), "auth", "can-i", "get", "configmaps/payment-keys", "-n", "shop", exporter);
        assertEquals("yes", answer(named));
        assertEquals("no", answer(other));
    }
}
