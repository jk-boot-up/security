package com.jk.explore.security.policyascode;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** The requests the bank's policy is tested with, each with the rules that a careful reviewer says it breaks. */
public final class Cases {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** A request and the rules (R1, R2, ...) a reviewer expects to see denied. Empty means allowed. */
    public record Case(String name, ObjectNode object, Set<String> expected) {
    }

    private Cases() {
    }

    public static ObjectNode good() {
        return (ObjectNode) JSON.readTree("""
                {"kind": "Deployment",
                 "metadata": {"name": "payments", "namespace": "bank", "labels": {"owner": "payments-team"}},
                 "spec": {"template": {"spec": {"containers": [{
                   "name": "payments", "image": "registry.bank.example/payments:2.3.1",
                   "securityContext": {"runAsNonRoot": true},
                   "resources": {"limits": {"cpu": "500m", "memory": "256Mi"}}}]}}}}""");
    }

    static ObjectNode container(ObjectNode o) {
        return (ObjectNode) o.get("spec").get("template").get("spec").get("containers").get(0);
    }

    static Case of(String name, Consumer<ObjectNode> change, String... expected) {
        ObjectNode o = good();
        change.accept(o);
        return new Case(name, o, Set.of(expected));
    }

    public static List<Case> all() {
        return List.of(
                of("good", o -> { }),
                of("foreign registry", o -> container(o).put("image", "docker.io/library/nginx:1.27"), "R2"),
                of("latest tag", o -> container(o).put("image", "registry.bank.example/payments:latest"), "R8"),
                of("no security context", o -> container(o).remove("securityContext"), "R3"),
                of("privileged", o -> container(o).putObject("securityContext").put("runAsNonRoot", true).put("privileged", true), "R4"),
                of("no limits", o -> container(o).remove("resources"), "R5"),
                of("no owner label", o -> ((ObjectNode) o.get("metadata")).remove("labels"), "R6"),
                of("host network", o -> ((ObjectNode) o.get("spec").get("template").get("spec")).put("hostNetwork", true), "R7"),
                of("cluster role binding", o -> o.put("kind", "ClusterRoleBinding"), "R1"),
                of("default namespace", o -> ((ObjectNode) o.get("metadata")).put("namespace", "default"), "R9"),
                of("three mistakes", o -> {
                    container(o).put("image", "docker.io/library/nginx:latest");
                    container(o).remove("securityContext");
                }, "R2", "R3", "R8"),
                of("an empty object", o -> o.removeAll(), "R1"),
                of("no containers", o -> ((ObjectNode) o.get("spec").get("template").get("spec")).remove("containers"), "R10"));
    }

    /** The rule numbers in the messages (R2, R5, ...). */
    public static Set<String> rules(List<String> messages) {
        return messages.stream().map(m -> m.substring(0, m.indexOf(' '))).collect(java.util.stream.Collectors.toSet());
    }
}
