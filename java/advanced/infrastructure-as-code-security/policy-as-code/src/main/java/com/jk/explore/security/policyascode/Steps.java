package com.jk.explore.security.policyascode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * A teaching class: each step of how Rego reads a missing field is written as its own rule, so that opa shows
 * the value of every step. A rule whose body cannot be satisfied is left out of the result: undefined.
 */
public final class Steps {

    static final String MODULE = """
            package steps

            import data.main

            kind := main.object.kind

            kind_not_allowed := true if not main.object.kind in main.allowed_kinds

            no_kind := true if not main.object.kind

            containers := count(main.containers)

            no_containers := count(main.containers) == 0

            run_as_non_root := c.securityContext.runAsNonRoot if some c in main.containers

            naive_compare := c.securityContext.runAsNonRoot == false if some c in main.containers

            required_check := true if {
            	some c in main.containers
            	not c.securityContext.runAsNonRoot == true
            }
            """;

    /** A step: the Rego expression as the policy writes it, and the rule above that computes it. */
    public record Step(String expression, String rule) {
    }

    public static final List<Step> EMPTY_OBJECT = List.of(
            new Step("object.kind", "kind"),
            new Step("not object.kind in allowed_kinds", "kind_not_allowed"),
            new Step("not object.kind", "no_kind"));

    public static final List<Step> NO_CONTAINERS = List.of(
            new Step("count(containers)", "containers"),
            new Step("count(containers) == 0", "no_containers"));

    public static final List<Step> NO_SECURITY_CONTEXT = List.of(
            new Step("c.securityContext.runAsNonRoot", "run_as_non_root"),
            new Step("c.securityContext.runAsNonRoot == false", "naive_compare"),
            new Step("not c.securityContext.runAsNonRoot == true", "required_check"));

    private Steps() {
    }

    /** The value of each step for this input, as opa prints it, or "undefined". */
    public static Map<Step, String> values(Path policy, List<Step> steps, JsonNode input, Path work)
            throws IOException, InterruptedException {
        Path module = Files.createDirectories(work).resolve("steps.rego");
        Files.writeString(module, MODULE);
        JsonNode result = Opa.eval(List.of(policy.resolve("main.rego"), module), "data.steps", input);
        Map<Step, String> out = new LinkedHashMap<>();
        for (Step s : steps) {
            out.put(s, show(result == null ? null : result.get(s.rule())));
        }
        return out;
    }

    /** How a value is shown: strings as they are, anything else as JSON, a missing value as "undefined". */
    static String show(JsonNode value) {
        return value == null ? "undefined" : value.isString() ? value.asString() : value.toString();
    }
}
