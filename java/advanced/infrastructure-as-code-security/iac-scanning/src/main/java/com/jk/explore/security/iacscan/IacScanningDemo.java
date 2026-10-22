package com.jk.explore.security.iacscan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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

    static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        out.append("--- our rules\n");
        List<Finding> first = Engine.scan(Manifests.FIRST);
        first.forEach(f -> out.append(f).append('\n'));
        out.append("first version: " + first.size() + " findings; after the fixes:\n");
        List<Finding> fixed = Engine.scan(Manifests.FIXED);
        fixed.forEach(f -> out.append(f).append('\n'));

        out.append("--- testing the rules\n");
        long passing = Rules.ALL.stream().filter(r -> RuleTester.check(r).isEmpty()).count();
        out.append(passing + " of " + Rules.ALL.size() + " rules fire on their bad sample and stay quiet on their good one\n");
        Rule mistaken = new Rule("K3", "image is latest", c -> c.container().path("image").asString("").endsWith(":latest"), "image: shop/orders", "image: shop/web:1.8.2");
        out.append("a rule that only knows :latest: " + RuleTester.check(mistaken) + "\n");

        out.append("--- three scanners on the same files\n");
        Venv venv = new Venv(Path.of("build"));
        Path work = Path.of("build/k");
        Path firstDir = Scanners.write(work.resolve("first"), Manifests.FIRST);
        Path fixedDir = Scanners.write(work.resolve("fixed"), Manifests.FIXED);
        Set<String> ck = Scanners.checkov(venv, firstDir);
        Set<String> tv = Scanners.trivy(firstDir);
        Set<String> kx = Scanners.kics(firstDir);
        Set<String> ckFixed = Scanners.checkov(venv, fixedDir);
        Set<String> tvFixed = Scanners.trivy(fixedDir);
        Set<String> kxFixed = Scanners.kics(fixedDir);
        for (Rule r : Rules.ALL) {
            out.append(String.format("%-3s %-34s checkov=%-3s trivy=%-3s kics=%s%n", r.id(), r.title(),
                    yes(Scanners.catches(ck, Scanners.CHECKOV.get(r.id()))), yes(Scanners.catches(tv, Scanners.TRIVY.get(r.id()))), yes(Scanners.catches(kx, Scanners.KICS.get(r.id())))));
        }
        out.append("the fixed files still draw advice: checkov=" + yes(!ckFixed.isEmpty()) + " trivy=" + yes(!tvFixed.isEmpty()) + " kics=" + yes(!kxFixed.isEmpty()) + "\n");
        out.append("the password written in the file is flagged by: checkov=" + yes(ck.contains("CKV_K8S_35")) + " trivy=" + yes(tv.contains("KSV-0109")) + " kics=" + yes(kx.contains("Secrets As Environment Variables")) + "\n");
        out.append("the password read from a Secret is flagged by: checkov=" + yes(ckFixed.contains("CKV_K8S_35")) + " trivy=" + yes(tvFixed.contains("KSV-0109")) + " kics=" + yes(kxFixed.contains("Secrets As Environment Variables")) + "\n");

        out.append("--- exceptions\n");
        LocalDate today = LocalDate.of(2026, 10, 22);
        Waiver agent = new Waiver("K6", "DaemonSet/metrics-agent", "platform team", "reads node counters from the host network", LocalDate.of(2026, 12, 31));
        out.append("with a waiver that ends 2026-12-31, on 2026-10-22: " + Waiver.apply(fixed, List.of(agent), today).size() + " findings\n");
        out.append("the same waiver on 2027-01-01: " + Waiver.apply(fixed, List.of(agent), LocalDate.of(2027, 1, 1)).size() + " finding, and " + Waiver.stale(List.of(agent), LocalDate.of(2027, 1, 1)).size() + " stale waiver\n");
        Waiver noOwner = new Waiver("K6", "DaemonSet/metrics-agent", "", "needed", LocalDate.of(2026, 12, 31));
        out.append("a waiver with no owner hides: " + (fixed.size() - Waiver.apply(fixed, List.of(noOwner), today).size()) + "\n");
        Waiver other = new Waiver("K6", "DaemonSet/other", "platform team", "needed", LocalDate.of(2026, 12, 31));
        out.append("a waiver for another resource hides: " + (fixed.size() - Waiver.apply(fixed, List.of(other), today).size()) + "\n");

        String annotated = Manifests.FIXED.replace("kind: DaemonSet\nmetadata:\n  name: metrics-agent\n",
                "kind: DaemonSet\nmetadata:\n  name: metrics-agent\n  annotations:\n    checkov.io/skip1: CKV_K8S_19=reads node counters from the host network\n");
        Path annotatedDir = Scanners.write(work.resolve("annotated"), annotated);
        out.append("checkov with an annotation in the file: CKV_K8S_19 still fails=" + yes(Scanners.checkov(venv, annotatedDir).contains("CKV_K8S_19")) + " (before: " + yes(ckFixed.contains("CKV_K8S_19")) + ")\n");
        Path ignore = Files.writeString(work.resolve("ignore.trivyignore"), "KSV-0009\n");
        out.append("trivy with an ignore file: KSV-0009 still fails=" + yes(Scanners.trivy(fixedDir, "--ignorefile", ignore.toString()).contains("KSV-0009")) + " (before: " + yes(tvFixed.contains("KSV-0009")) + ")\n");

        out.append("--- a baseline in the build\n");
        List<Finding> now = Engine.scan(Manifests.FIXED + "---\n" + CART);
        Baseline.newFindings(now, fixed).forEach(f -> out.append("new: " + f + "\n"));
        out.append("fixed since the baseline: " + Baseline.fixed(now, fixed).size() + "\n");

        out.append("--- the bill\n");
        out.append("all three scanners miss the same thing (a password written in the file) and two flag the good practice\n");
        out.append("an exception without an owner and an end date is a hole\n");
        return out.toString();
    }
}
