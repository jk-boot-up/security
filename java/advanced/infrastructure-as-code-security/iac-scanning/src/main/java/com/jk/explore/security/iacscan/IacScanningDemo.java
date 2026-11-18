package com.jk.explore.security.iacscan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Five acts on an online store's Kubernetes manifests: eight rules written in Java, the rules' own tests, three scanners
 * on the same files, exceptions, and a baseline in the build. Dates are fixed, so the output is the same on every run.
 */
public class IacScanningDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static final String CART = """
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: cart
            spec:
              template:
                spec:
                  containers:
                    - name: cart
                      image: shop/cart:latest
            """;

    static final LocalDate TODAY = LocalDate.of(2026, 10, 22);
    static final LocalDate LATER = LocalDate.of(2027, 1, 1);
    static final String ANNOTATION = "checkov.io/skip1: CKV_K8S_19=reads node counters";

    static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    static void lines(StringBuilder out, List<String> lines) {
        lines.forEach(l -> out.append("    ").append(l).append('\n'));
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        List<Finding> first = Engine.scan(Manifests.FIRST);
        List<Finding> fixed = Engine.scan(Manifests.FIXED);
        actOne(out, first, fixed);
        actTwo(out);
        Venv venv = new Venv(Path.of("build"));
        Path work = Path.of("build/k");
        Path firstDir = Scanners.write(work.resolve("first"), Manifests.FIRST);
        Path fixedDir = Scanners.write(work.resolve("fixed"), Manifests.FIXED);
        Set<String> ckFixed = Scanners.checkov(venv, fixedDir);
        Set<String> tvFixed = Scanners.trivy(fixedDir);
        actThree(out, venv, firstDir, fixedDir, ckFixed, tvFixed);
        actFour(out, venv, work, fixed, fixedDir, ckFixed, tvFixed);
        actFive(out, fixed);
        return out.toString();
    }

    private static void actOne(StringBuilder out, List<Finding> first, List<Finding> fixed) {
        out.append("ONE. Eight rules in Java, on the store's manifests.\n");
        out.append("  THE MANIFESTS (what each container sets, first version):\n");
        lines(out, Engine.settings(Manifests.FIRST, 58));
        out.append("  THE RULES (a question about one container):\n");
        Rules.ALL.forEach(r -> out.append("    ").append(r.id()).append("  ").append(r.title()).append('\n'));
        out.append("  THE FINDINGS (x: the container breaks the rule):\n");
        lines(out, Engine.matrix(Manifests.FIRST, first));
        List<Long> perContainer = Engine.matrix(Manifests.FIRST, first).stream().skip(1)
                .map(l -> Long.parseLong(l.substring(l.lastIndexOf(' ') + 1))).toList();
        out.append("  HOW THE FINDINGS ADD UP:\n");
        out.append("    ").append(String.join(" + ", perContainer.stream().map(String::valueOf).toList()))
                .append(" = ").append(first.size()).append(" findings\n");
        out.append("  AFTER THE FIXES (tag, limits, read-only, a Secret):\n");
        lines(out, Engine.matrix(Manifests.FIXED, fixed));
        out.append("    ").append(fixed.size()).append(" finding: ").append(fixed.getFirst().rule()).append(" on ")
                .append(fixed.getFirst().resource()).append('\n');
    }

    private static void actTwo(StringBuilder out) {
        out.append("TWO. Testing the rules with their own samples.\n");
        out.append("  THE SAMPLES (bad: must fire; good: must stay quiet):\n");
        for (Rule r : Rules.ALL) {
            out.append(String.format("    %-3s bad   %s%n", r.id(), r.bad()));
            out.append(String.format("        good  %s%n", r.good().replace(", valueFrom: ", ",\n              valueFrom: ")));
        }
        out.append("  THE RESULT:\n");
        long passing = Rules.ALL.stream().filter(r -> RuleTester.check(r).isEmpty()).count();
        out.append("    ").append(passing).append(" of ").append(Rules.ALL.size()).append(" rules fire on bad and stay quiet on good\n");
        Rule mistaken = new Rule("K3", "image is latest", c -> c.container().path("image").asString("").endsWith(":latest"), "image: shop/orders", "image: shop/web:1.8.2");
        out.append("  A RULE WITH A MISTAKE (K3 written as: ends with :latest):\n");
        out.append("    bad sample  ").append(mistaken.bad()).append('\n');
        out.append("  THE RESULT:\n");
        out.append("    ").append(RuleTester.check(mistaken)).append(": the test fails\n");
        String image = mistaken.bad().substring("image: ".length());
        out.append("  HOW THE REAL K3 READS ").append(image).append(":\n");
        out.append("    last part   ").append(image.substring(image.lastIndexOf('/') + 1)).append('\n');
        String tag = Rules.tag(image);
        out.append("    tag         ").append(tag.isEmpty() ? "none, so Kubernetes pulls latest" : tag).append('\n');
        out.append("    K3 fires    ").append(yes(RuleTester.fires(Rules.ALL.get(2), mistaken.bad()))).append('\n');
    }

    private static void actThree(StringBuilder out, Venv venv, Path firstDir, Path fixedDir, Set<String> ckFixed, Set<String> tvFixed) throws Exception {
        Set<String> ck = Scanners.checkov(venv, firstDir);
        Set<String> tv = Scanners.trivy(firstDir);
        Set<String> kx = Scanners.kics(firstDir);
        Set<String> kxFixed = Scanners.kics(fixedDir);
        out.append("THREE. Three scanners on the same files.\n");
        out.append("  THE TOOLS (pinned, so the checks do not change):\n");
        out.append("    checkov ").append(Venv.CHECKOV).append(", trivy ").append(Scanners.TRIVY_VERSION)
                .append(", kics by image digest\n");
        out.append("  OUR RULE, AND THE CHECK EACH TOOL FIRED (first files):\n");
        out.append(String.format("    %-3s %-11s %-9s %s%n", "", "checkov", "trivy", "kics"));
        int all = 0;
        String none = "";
        for (Rule r : Rules.ALL) {
            String c = Scanners.first(ck, Scanners.CHECKOV.get(r.id()));
            String t = Scanners.first(tv, Scanners.TRIVY.get(r.id()));
            String k = Scanners.first(kx, Scanners.KICS.get(r.id()));
            out.append(String.format("    %-3s %-11s %-9s %s%n", r.id(), c, t, k));
            if (!c.equals("-") && !t.equals("-") && !k.equals("-")) {
                all++;
            }
            if (c.equals("-") && t.equals("-") && k.equals("-")) {
                none = r.id() + " " + r.title();
            }
        }
        out.append("  HOW THE COMPARISON IS MADE:\n");
        out.append("    found by all three  ").append(all).append(" of ").append(Rules.ALL.size()).append('\n');
        out.append("    found by none       ").append(none).append('\n');
        out.append("  THE PASSWORD, TWO WAYS (CKV_K8S_35, KSV-0109, KICS):\n");
        out.append(String.format("    %-22s checkov %-3s trivy %-3s kics %s%n", "written in the file", yes(ck.contains("CKV_K8S_35")),
                yes(tv.contains("KSV-0109")), yes(kx.contains("Secrets As Environment Variables"))));
        out.append(String.format("    %-22s checkov %-3s trivy %-3s kics %s%n", "read from a Secret", yes(ckFixed.contains("CKV_K8S_35")),
                yes(tvFixed.contains("KSV-0109")), yes(kxFixed.contains("Secrets As Environment Variables"))));
        out.append("  THE FIXED FILES (checks that still fail):\n");
        out.append("    checkov ").append(ckFixed.size()).append(", trivy ").append(tvFixed.size()).append(", kics ")
                .append(kxFixed.size()).append(": advice beyond our eight rules\n");
    }

    private static void actFour(StringBuilder out, Venv venv, Path work, List<Finding> fixed, Path fixedDir, Set<String> ckFixed, Set<String> tvFixed) throws Exception {
        out.append("FOUR. Exceptions: a waiver with an owner and an end.\n");
        out.append("  THE FINDING (fixed files):\n");
        out.append("    ").append(fixed.getFirst()).append('\n');
        Waiver agent = new Waiver("K6", "DaemonSet/metrics-agent", "platform team", "reads node counters", LocalDate.of(2026, 12, 31));
        out.append("  THE WAIVER:\n");
        out.append("    for     ").append(agent.rule()).append(" on ").append(agent.resource()).append('\n');
        out.append("    owner   ").append(agent.owner()).append('\n');
        out.append("    reason  ").append(agent.reason()).append(" from the host network\n");
        out.append("    ends    ").append(agent.expires()).append('\n');
        Waiver noOwner = new Waiver("K6", "DaemonSet/metrics-agent", "", "needed", agent.expires());
        Waiver other = new Waiver("K6", "DaemonSet/other", "platform team", "needed", agent.expires());
        out.append("  THE RESULT (findings left, by day and waiver):\n");
        out.append(String.format("    %s  this waiver          %d%n", TODAY, Waiver.apply(fixed, List.of(agent), TODAY).size()));
        out.append(String.format("    %s  this waiver          %d, and %d stale waiver%n", LATER, Waiver.apply(fixed, List.of(agent), LATER).size(),
                Waiver.stale(List.of(agent), LATER).size()));
        out.append(String.format("    %s  no owner             %d%n", TODAY, Waiver.apply(fixed, List.of(noOwner), TODAY).size()));
        out.append(String.format("    %s  another resource     %d%n", TODAY, Waiver.apply(fixed, List.of(other), TODAY).size()));
        String annotated = Manifests.FIXED.replace("kind: DaemonSet\nmetadata:\n  name: metrics-agent\n",
                "kind: DaemonSet\nmetadata:\n  name: metrics-agent\n  annotations:\n    " + ANNOTATION + "\n");
        Path annotatedDir = Scanners.write(work.resolve("annotated"), annotated);
        Path ignore = Files.writeString(work.resolve("ignore.trivyignore"), "KSV-0009\n");
        out.append("  THE SAME INSIDE THE TOOLS:\n");
        out.append("    checkov  annotation ").append(ANNOTATION.substring(0, ANNOTATION.indexOf('='))).append('\n');
        out.append("    trivy    .trivyignore with the line KSV-0009\n");
        out.append("  THE RESULT (does the host network check still fail?):\n");
        out.append("    CKV_K8S_19  before ").append(yes(ckFixed.contains("CKV_K8S_19"))).append("   after ")
                .append(yes(Scanners.checkov(venv, annotatedDir).contains("CKV_K8S_19"))).append('\n');
        out.append("    KSV-0009    before ").append(yes(tvFixed.contains("KSV-0009"))).append("   after ")
                .append(yes(Scanners.trivy(fixedDir, ignore).contains("KSV-0009"))).append('\n');
    }

    private static void actFive(StringBuilder out, List<Finding> fixed) {
        out.append("FIVE. A baseline in the build.\n");
        out.append("  THE BASELINE (the findings accepted so far):\n");
        fixed.forEach(f -> out.append("    ").append(f).append('\n'));
        out.append("  THE CHANGE: a new Deployment/cart, image shop/cart:latest\n");
        List<Finding> now = Engine.scan(Manifests.FIXED + "---\n" + CART);
        List<Finding> added = Baseline.newFindings(now, fixed);
        out.append("  THE SCAN NOW:\n");
        out.append("    ").append(now.size()).append(" findings\n");
        out.append("  HOW THE BUILD DECIDES:\n");
        out.append("    ").append(now.size()).append(" now - ").append(now.size() - added.size()).append(" accepted = ")
                .append(added.size()).append(" new, so the build fails\n");
        added.forEach(f -> out.append("    new  ").append(f).append('\n'));
        out.append("    fixed since the baseline: ").append(Baseline.fixed(now, fixed).size()).append('\n');
        out.append("  WHAT IS LEFT FOR PEOPLE:\n");
        out.append("    blind spot  no tool flags a password written in the file\n");
        out.append("    noise       two flag the good practice of a Secret\n");
        out.append("    exceptions  a waiver needs an owner and an end date\n");
    }
}
