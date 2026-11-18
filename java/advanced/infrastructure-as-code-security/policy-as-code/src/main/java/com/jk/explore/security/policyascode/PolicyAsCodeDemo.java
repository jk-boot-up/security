package com.jk.explore.security.policyascode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    static String expected(Cases.Case c) {
        return c.expected().isEmpty() ? "allow" : "deny " + String.join(" ", new TreeSet<>(c.expected()));
    }

    static Cases.Case named(String name) {
        return Cases.all().stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow();
    }

    /** The decisions of the policy in that folder, as rows "name  reviewer  policy"; returns how many agree. */
    static int agree(Path policy, List<String> rows, boolean onlyDisagreements) throws Exception {
        int n = 0;
        for (Cases.Case c : Cases.all()) {
            List<String> d = Opa.deny(policy, "main", c.object());
            boolean same = Cases.rules(d).equals(c.expected());
            n += same ? 1 : 0;
            if (!same || !onlyDisagreements) {
                rows.add(String.format("    %-22s%-15s%s%n", c.name(), expected(c), describe(d)));
            }
        }
        return n;
    }

    static void steps(StringBuilder out, Map<Steps.Step, String> values) {
        values.forEach((s, v) -> out.append(String.format("    %-44s%s%n", s.expression(), v)));
    }

    public static String run() throws Exception {
        Opa.requirePinnedVersions();
        StringBuilder out = new StringBuilder();
        String source = Files.readString(POLICY.resolve("main.rego"));
        int rules = Mutants.count(source);
        int cases = Cases.all().size();
        Path work = Path.of("build/demo");

        out.append("ONE. Deny by default: twelve rules, thirteen requests.\n");
        out.append("  THE RULES IN policy/main.rego (each one a deny rule):\n");
        for (int i = 0; i < rules; i += 2) {
            out.append(String.format("    %-31s%s%n", Mutants.label(source, i), i + 1 < rules ? Mutants.label(source, i + 1) : ""));
        }
        Cases.Case three = named("three mistakes");
        JsonNode c3 = Cases.container(three.object());
        out.append("  THE REQUEST \"three mistakes\" (the good one, changed):\n");
        out.append("    image            " + c3.get("image").asString() + "\n");
        out.append("    securityContext  " + (c3.has("securityContext") ? c3.get("securityContext") : "(none)") + "\n");
        out.append("  THE ANSWER (the messages of data.main.deny):\n");
        List<String> d3 = Opa.deny(POLICY, "main", three.object());
        d3.forEach(m -> out.append("    " + m + "\n"));
        out.append("    allow  " + d3.isEmpty() + " (" + d3.size() + " messages; allow needs 0)\n");
        out.append("  THE DECISIONS (what a reviewer expects, what the policy says):\n");
        out.append(String.format("    %-22s%-15s%s%n", "request", "reviewer", "policy"));
        List<String> rows = new ArrayList<>();
        int ok = agree(POLICY, rows, false);
        rows.forEach(out::append);
        out.append("  THE RESULT:\n");
        out.append("    the policy and the reviewer agree on " + ok + " of " + cases + " requests\n");

        out.append("TWO. The holes: a missing field denies nothing.\n");
        Path draft = Files.createDirectories(work.resolve("draft"));
        Files.writeString(draft.resolve("main.rego"), Mutants.disable(Mutants.disable(source, rules - 1), rules - 2));
        out.append("  BEFORE THE FIX (the first draft, without the last two rules):\n");
        out.append(String.format("    %-22s%-15s%s%n", "request", "reviewer", "draft"));
        List<String> holes = new ArrayList<>();
        int draftOk = agree(draft, holes, true);
        holes.forEach(out::append);
        out.append("    the first draft agrees on " + draftOk + " of " + cases + "\n");
        out.append("  HOW THE EMPTY OBJECT IS READ (the value of each step):\n");
        steps(out, Steps.values(POLICY, Steps.EMPTY_OBJECT, named("an empty object").object(), work));
        out.append("  HOW THE DEPLOYMENT WITH NO CONTAINERS IS READ:\n");
        steps(out, Steps.values(POLICY, Steps.NO_CONTAINERS, named("no containers").object(), work));
        out.append("  AFTER THE FIX (rules that ask for what is required):\n");
        out.append(String.format("    %-31s%s%n", Mutants.label(source, rules - 2), "not object.kind"));
        out.append(String.format("    %-31s%s%n", Mutants.label(source, rules - 1), "count(containers) == 0"));
        out.append("    the policy agrees on " + ok + " of " + cases + "\n");
        ObjectNode noContext = Cases.good();
        Cases.container(noContext).remove("securityContext");
        Cases.container(noContext).remove("resources");
        out.append("  THE SAME TRAP IN A LIST OF WHAT IS FORBIDDEN:\n");
        out.append("    the request: the good one, no securityContext, no limits\n");
        steps(out, Steps.values(POLICY, Steps.NO_SECURITY_CONTEXT, noContext, work));
        out.append("    naive.rego (== false)        " + describe(Opa.deny(POLICY, "naive", noContext)) + "\n");
        out.append("    main.rego (not ... == true)  " + describe(Opa.deny(POLICY, "main", noContext)) + "\n");

        out.append("THREE. Testing the policy, and testing its tests.\n");
        Opa.Tests all = Opa.test(null, List.of(POLICY));
        out.append("  THE TESTS (policy/main_test.rego):\n");
        out.append("    opa test: " + all.passed() + " of " + all.total() + " tests pass\n");
        out.append("  HOW A MUTANT IS MADE (rule 5 of " + rules + ", " + Mutants.label(source, 4) + "):\n");
        out.append("    BEFORE  deny contains msg if {\n");
        out.append("    AFTER   deny_off contains msg if {\n");
        out.append("    nobody asks for deny_off, so the rule denies nothing\n");
        Path mutants = work.resolve("mutants");
        Mutants.Score weak = Mutants.score(POLICY, "test_good_is_allowed|test_r1_", mutants);
        Mutants.Score basic = Mutants.score(POLICY, "test_good_is_allowed|test_r1_unknown|test_r2_|test_r3_|test_r4_"
                + "|test_r5_no_limits|test_r6_|test_r7_|test_r8_|test_r9_|test_admission", mutants);
        Mutants.Score full = Mutants.score(POLICY, null, mutants);
        out.append("  WHICH MUTANTS THE TESTS NOTICE (one rule switched off):\n");
        out.append(String.format("    %-31s%-10s%-10s%s%n", "rule switched off", weak.tests() + " tests", basic.tests() + " tests", full.tests() + " tests"));
        for (int i = 0; i < rules; i++) {
            out.append(String.format("    %-31s%-10s%-10s%s%n", Mutants.label(source, i),
                    yes(weak.noticed().get(i)), yes(basic.noticed().get(i)), yes(full.noticed().get(i))));
        }
        out.append("  THE RESULT:\n");
        out.append(String.format("    %-31s%-10s%-10s%s%n", "noticed, of " + rules,
                weak.killed(), basic.killed(), full.killed()));

        out.append("FOUR. In the cluster and in the build.\n");
        ObjectNode bad = Cases.good();
        Cases.container(bad).put("image", "docker.io/library/nginx:latest");
        JsonNode review = Admission.review(bad);
        out.append("  THE REQUEST (an AdmissionReview from the API server):\n");
        out.append("    apiVersion  " + review.get("apiVersion").asString() + "\n");
        out.append("    request     uid " + review.get("request").get("uid").asString() + ", a " + bad.get("kind").asString()
                + " named " + bad.get("metadata").get("name").asString() + "\n");
        out.append("    image       " + Cases.container(bad).get("image").asString() + "\n");
        Admission.Response denied = Admission.handle(POLICY, bad);
        out.append("  THE RESPONSE:\n");
        out.append("    allowed     " + denied.allowed() + "\n");
        for (String reason : denied.message().split("; ")) {
            out.append("    " + reason + "\n");
        }
        out.append("    (the good Deployment: allowed " + Admission.handle(POLICY, Cases.good()).allowed() + ")\n");

        Path fleet = Files.createDirectories(work.resolve("fleet"));
        YAMLMapper yaml = YAMLMapper.builder().build();
        ObjectNode latest = Cases.good();
        Cases.container(latest).put("image", "registry.bank.example/payments:latest");
        Map<String, ObjectNode> files = new java.util.LinkedHashMap<>();
        files.put("a-payments.yaml", Cases.good());
        files.put("b-cart.yaml", latest);
        files.put("c-legacy.yaml", bad);
        out.append("  THE FILES IN THE BUILD (conftest test build/demo/fleet):\n");
        for (var f : files.entrySet()) {
            Files.writeString(fleet.resolve(f.getKey()), yaml.writeValueAsString(f.getValue()));
            out.append(String.format("    %-17s%s%n", f.getKey(), Cases.container(f.getValue()).get("image").asString()));
        }
        Opa.Conftest ct = Opa.conftest(POLICY, fleet);
        out.append("  WHAT CONFTEST SAYS:\n");
        for (String name : files.keySet()) {
            TreeSet<String> ids = new TreeSet<>();
            ct.failures().stream().filter(f -> f.startsWith(name + ": ")).forEach(f -> ids.add(f.substring(name.length() + 2).split(" ")[0]));
            out.append(String.format("    %-17s%s%n", name, ids.isEmpty() ? "pass" : "fail " + String.join(" ", ids)));
        }
        out.append("    exit " + ct.exit() + (ct.exit() == 0 ? ": the build goes on" : ": the build stops") + "\n");

        Path onlyGood = Files.createDirectories(work.resolve("only-good"));
        Files.writeString(onlyGood.resolve("payments.yaml"), yaml.writeValueAsString(Cases.good()));
        String rule = """

                %s contains msg if {
                	some c in containers
                	not c.securityContext.readOnlyRootFilesystem == true
                	msg := sprintf("R11 container %%v has a writable root filesystem", [c.name])
                }
                """;
        out.append("  A NEW RULE, R11: a read-only root filesystem\n");
        out.append("    payments.yaml does not set readOnlyRootFilesystem\n");
        for (String level : new String[] {"warn", "deny"}) {
            Path dir = Files.createDirectories(work.resolve("rollout-" + level));
            Files.writeString(dir.resolve("main.rego"), source + rule.formatted(level));
            Opa.Conftest r = Opa.conftest(dir, onlyGood);
            out.append(String.format("  %s%n    exit %d, %s, %s: the build %s%n",
                    level.equals("warn") ? "FIRST, AS A WARNING (warn contains msg):" : "LATER, AS A DENIAL (deny contains msg):",
                    r.exit(), count(r.warnings().size(), "warning"), count(r.failures().size(), "failure"), r.exit() == 0 ? "goes on" : "stops"));
        }
        return out.toString();
    }

    static String count(int n, String word) {
        return n + " " + word + (n == 1 ? "" : "s");
    }

    static String yes(boolean b) {
        return b ? "yes" : "no";
    }
}
