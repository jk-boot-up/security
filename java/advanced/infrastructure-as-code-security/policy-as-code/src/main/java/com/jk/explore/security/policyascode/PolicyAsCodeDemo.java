package com.jk.explore.security.policyascode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class PolicyAsCodeDemo {

    static final Path POLICY = Path.of("policy");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String describe(List<String> denials) {
        return denials.isEmpty() ? "allow" : "deny " + String.join(" ", new TreeSet<>(Cases.rules(denials)));
    }

    /** How many cases the policy in that folder decides as the reviewer expects. */
    static int agree(Path policy, StringBuilder out) throws Exception {
        int n = 0;
        for (Cases.Case c : Cases.all()) {
            List<String> d = Opa.deny(policy, "main", c.object());
            boolean same = Cases.rules(d).equals(c.expected());
            n += same ? 1 : 0;
            if (out != null) {
                out.append(String.format("%-22s %-18s%s%n", c.name(), describe(d), same ? "" : " <- the reviewer expected " + (c.expected().isEmpty() ? "deny" : "a denial of " + new TreeSet<>(c.expected()))));
            }
        }
        return n;
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        int cases = Cases.all().size();
        out.append("--- deny by default\n");
        int ok = agree(POLICY, out);
        out.append("the policy and the reviewer agree on " + ok + " of " + cases + " requests\n");

        out.append("--- the holes\n");
        String source = Files.readString(POLICY.resolve("main.rego"));
        Path draft = Files.createDirectories(Path.of("build/draft"));
        Files.writeString(draft.resolve("main.rego"), Mutants.disable(Mutants.disable(source, Mutants.count(source) - 1), Mutants.count(source) - 2));
        StringBuilder holes = new StringBuilder();
        int draftOk = agree(draft, holes);
        holes.toString().lines().filter(l -> l.contains("<-")).forEach(l -> out.append("draft: ").append(l).append('\n'));
        out.append("the first draft agrees on " + draftOk + " of " + cases + "\n");
        ObjectNode noContext = Cases.good();
        Cases.container(noContext).remove("securityContext");
        Cases.container(noContext).remove("resources");
        out.append("a list of what is forbidden (naive.rego) on a container with no securityContext and no limits: " + describe(Opa.deny(POLICY, "naive", noContext)) + "\n");
        out.append("the same container against main.rego: " + describe(Opa.deny(POLICY, "main", noContext)) + "\n");

        out.append("--- testing the policy\n");
        Opa.Tests all = Opa.test(null, List.of(POLICY));
        out.append("opa test: " + all.passed() + " of " + all.total() + " tests pass\n");
        int mutants = Mutants.count(source);
        Path work = Path.of("build/mutants");
        String weak = "test_good_is_allowed|test_r1_";
        String basic = "test_good_is_allowed|test_r1_unknown|test_r2_|test_r3_|test_r4_|test_r5_no_limits|test_r6_|test_r7_|test_r8_|test_r9_|test_admission";
        out.append("switching off one rule at a time (" + mutants + " mutants), tests notice: with 3 tests " + Mutants.killed(POLICY, weak, work) + ", with one test for each rule " + Mutants.killed(POLICY, basic, work) + ", with all " + Mutants.killed(POLICY, null, work) + "\n");

        out.append("--- in the cluster and in the build\n");
        ObjectNode bad = Cases.good();
        Cases.container(bad).put("image", "docker.io/library/nginx:latest");
        Admission.Response denied = Admission.handle(POLICY, bad);
        out.append("admission review of a bad Deployment: allowed=" + denied.allowed() + ", " + denied.message().split("; ").length + " reasons\n");
        out.append("admission review of the good one: allowed=" + Admission.handle(POLICY, Cases.good()).allowed() + "\n");

        Path fleet = Files.createDirectories(Path.of("build/fleet"));
        YAMLMapper yaml = YAMLMapper.builder().build();
        ObjectNode latest = Cases.good();
        Cases.container(latest).put("image", "registry.bank.example/payments:latest");
        Files.writeString(fleet.resolve("a-payments.yaml"), yaml.writeValueAsString(Cases.good()));
        Files.writeString(fleet.resolve("b-cart.yaml"), yaml.writeValueAsString(latest));
        Files.writeString(fleet.resolve("c-legacy.yaml"), yaml.writeValueAsString(bad));
        Opa.Conftest ct = Opa.conftest(POLICY, fleet);
        out.append("conftest on three files: exit " + ct.exit() + "\n");
        ct.failures().forEach(f -> out.append("  " + f + "\n"));

        Path onlyGood = Files.createDirectories(Path.of("build/only-good"));
        Files.writeString(onlyGood.resolve("payments.yaml"), yaml.writeValueAsString(Cases.good()));
        String rule = """

                %s contains msg if {
                	some c in containers
                	not c.securityContext.readOnlyRootFilesystem == true
                	msg := sprintf("R11 container %%v has a writable root filesystem", [c.name])
                }
                """;
        for (String level : new String[] {"warn", "deny"}) {
            Path dir = Files.createDirectories(Path.of("build/rollout-" + level));
            Files.writeString(dir.resolve("main.rego"), source + rule.formatted(level));
            Opa.Conftest r = Opa.conftest(dir, onlyGood);
            out.append("a new rule as " + level + ": exit " + r.exit() + ", " + r.warnings().size() + " warnings, " + r.failures().size() + " failures\n");
        }

        out.append("--- the bill\n");
        out.append("a policy is code: it is reviewed, versioned and tested, and its tests are tested\n");
        out.append("a missing field must mean deny; a rule must be rolled out as a warning first\n");
        return out.toString();
    }
}
