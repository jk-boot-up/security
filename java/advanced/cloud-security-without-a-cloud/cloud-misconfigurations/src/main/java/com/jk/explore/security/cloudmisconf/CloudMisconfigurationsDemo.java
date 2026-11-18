package com.jk.explore.security.cloudmisconf;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Five acts: a bank's first cloud setup on an AWS emulator and its 13 findings, the fixes, the same setup as files read by
 * Checkov and Trivy, the metadata endpoint attacked, and what neither the scanners nor the emulator can tell. Ids that
 * change on every run (instance, volume and token ids) are never printed.
 */
public class CloudMisconfigurationsDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Venv venv = new Venv(Path.of("build"));
        try (Venv.Moto moto = new Venv.Moto(venv)) {
            Cloud cloud = new Cloud(moto.url());
            Environment.deployFirstVersion(cloud);
            List<Finding> first = Scanner.scan(cloud);
            actOne(out, cloud, first);
            actTwo(out, cloud, first);
            actThree(out, venv);
            actFour(out);
            actFive(out, cloud);
        }
        return out.toString();
    }

    private static void actOne(StringBuilder out, Cloud cloud, List<Finding> findings) {
        out.append("ONE. The first version, deployed to the emulator.\n");
        out.append("  THE ACCOUNT (what the scanner read from Moto):\n");
        Scanner.read(cloud).lines().forEach(l -> out.append("    ").append(l).append('\n'));
        out.append("  THE SCAN (").append(findings.size()).append(" findings, worst first):\n");
        findings.forEach(f -> out.append("    ").append(f).append('\n'));
        out.append("  HOW THE FINDINGS ADD UP:\n");
        out.append("    by severity  ").append(bySeverity(findings)).append('\n');
        Map<String, Long> perRule = findings.stream().collect(Collectors.groupingBy(Finding::rule, TreeMap::new, Collectors.counting()));
        List<String> twice = IacScanners.RULES.stream().filter(r -> perRule.getOrDefault(r, 0L) == 2).toList();
        out.append("    by rule      ").append(perRule.size()).append(" rules; ").append(twice.size()).append(" fire twice: ")
                .append(perRule.size()).append(" + ").append(twice.size()).append(" = ").append(findings.size()).append('\n');
        out.append("    twice        ").append(String.join(", ", twice.subList(0, 2))).append(",\n");
        out.append("                 ").append(String.join(", ", twice.subList(2, twice.size()))).append('\n');
    }

    static String bySeverity(List<Finding> findings) {
        List<String> parts = Finding.SEVERITIES.stream()
                .map(s -> findings.stream().filter(f -> f.severity().equals(s)).count() + " " + s)
                .filter(p -> !p.startsWith("0 ")).toList();
        return String.join(" + ", parts) + " = " + findings.size();
    }

    private static void actTwo(StringBuilder out, Cloud cloud, List<Finding> before) {
        out.append("TWO. The fixes, made with the same SDK.\n");
        out.append("  BEFORE THE FIXES:\n");
        out.append("    ").append(bySeverity(before)).append(" findings\n");
        out.append("  THE FIXES (edit: in place; replace: delete and create):\n");
        List<Environment.Fix> fixes = Environment.harden(cloud);
        fixes.forEach(f -> out.append("    ").append(f).append('\n'));
        out.append("  AFTER THE FIXES (what the scanner read):\n");
        Scanner.read(cloud).lines().forEach(l -> out.append("    ").append(l).append('\n'));
        List<Finding> after = Scanner.scan(cloud);
        out.append("  THE SCAN AFTER THE FIXES:\n");
        out.append("    ").append(after.size()).append(" findings\n");
        long edits = fixes.stream().filter(f -> f.kind().equals("edit")).count();
        out.append("    ").append(fixes.size()).append(" changes = ").append(edits).append(" edits + ").append(fixes.size() - edits).append(" replacements\n");
    }

    private static void actThree(StringBuilder out, Venv venv) throws Exception {
        Path work = Path.of("build/tf");
        Path firstDir = Terraform.write(work.resolve("first"), Terraform.FIRST);
        Path fixedDir = Terraform.write(work.resolve("fixed"), Terraform.FIXED);
        IacScanners.Report ck1 = IacScanners.checkov(venv, firstDir);
        IacScanners.Report ck2 = IacScanners.checkov(venv, fixedDir);
        IacScanners.Report tv1 = IacScanners.trivy(firstDir);
        IacScanners.Report tv2 = IacScanners.trivy(fixedDir);
        out.append("THREE. Before deploy: the same setup as files, read by two scanners.\n");
        out.append("  THE FILES (main.tf, OpenTofu):\n");
        out.append("    first  ").append(Terraform.resources(Terraform.FIRST)).append(" resources: public buckets, Action *, 0.0.0.0/0\n");
        out.append("    fixed  ").append(Terraform.resources(Terraform.FIXED)).append(" resources: + access blocks, encryption, versions\n");
        out.append("  THE REPORTS (failed checks):\n");
        out.append(String.format("    %-15s first %2d   fixed %2d%n", "checkov " + Venv.CHECKOV, ck1.failed(), ck2.failed()));
        out.append(String.format("    %-15s first %2d   fixed %2d%n", "trivy " + IacScanners.TRIVY_VERSION, tv1.failed(), tv2.failed()));
        out.append("  OUR NINE RULES, AND THE CHECK EACH TOOL FIRED (first files):\n");
        out.append(String.format("    %-26s %-12s %s%n", "rule", "checkov", "trivy"));
        for (String rule : IacScanners.RULES) {
            out.append(String.format("    %-26s %-12s %s%n", rule, ck1.first(IacScanners.CHECKOV.get(rule)), tv1.first(IacScanners.TRIVY.get(rule))));
        }
        List<String> both = IacScanners.RULES.stream().filter(r -> ck1.caught(IacScanners.CHECKOV.get(r)) && tv1.caught(IacScanners.TRIVY.get(r))).toList();
        List<String> checkovOnly = IacScanners.RULES.stream().filter(r -> ck1.caught(IacScanners.CHECKOV.get(r)) && !tv1.caught(IacScanners.TRIVY.get(r))).toList();
        List<String> trivyOnly = IacScanners.RULES.stream().filter(r -> !ck1.caught(IacScanners.CHECKOV.get(r)) && tv1.caught(IacScanners.TRIVY.get(r))).toList();
        out.append("  HOW THE COMPARISON IS MADE:\n");
        out.append("    found by both   ").append(both.size()).append(" of ").append(IacScanners.RULES.size()).append('\n');
        out.append("    checkov only    ").append(checkovOnly.isEmpty() ? "none" : String.join(", ", checkovOnly)).append('\n');
        out.append("    trivy only      ").append(trivyOnly.isEmpty() ? "none" : String.join(", ", trivyOnly)).append('\n');
        List<String> sg = IacScanners.CHECKOV.get("sg-world-admin-port");
        List<String> sgT = IacScanners.TRIVY.get("sg-world-admin-port");
        for (String[] g : new String[][] {{"web", "22"}, {"database", "5432"}}) {
            String res = "aws_security_group." + g[0];
            out.append(String.format("    port %-4s on %-9s checkov %-3s trivy %s%n", g[1], g[0], yes(ck1.caughtOn(sg, res)), yes(tv1.caughtOn(sgT, res))));
        }
        out.append("  THE FIXED FILES:\n");
        List<String> leftC = IacScanners.RULES.stream().filter(r -> ck2.caught(IacScanners.CHECKOV.get(r))).toList();
        List<String> leftT = IacScanners.RULES.stream().filter(r -> tv2.caught(IacScanners.TRIVY.get(r))).toList();
        out.append("    rules left      checkov ").append(String.join(", ", leftC)).append('\n');
        out.append("                    trivy   ").append(String.join(", ", leftT)).append('\n');
        out.append("    why             ").append(ck2.first(IacScanners.CHECKOV.get("s3-no-encryption"))).append(" and ")
                .append(tv2.first(IacScanners.TRIVY.get("s3-no-encryption"))).append(" want a key of your own;\n");
        out.append("                    our rule accepts AES256\n");
        out.append("    other advice    checkov ").append(ck2.otherThan(IacScanners.CHECKOV.values())).append(", trivy ")
                .append(tv2.otherThan(IacScanners.TRIVY.values())).append(" (logging, monitoring...)\n");
    }

    static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    private static void actFour(StringBuilder out) throws Exception {
        out.append("FOUR. The metadata endpoint, reached through the web server.\n");
        String role = Imds.ROLE_PATH;
        try (Imds v1 = new Imds(false); Imds v2 = new Imds(true)) {
            out.append("  THE REQUEST (a feature that fetches a link for the visitor):\n");
            out.append("    GET ").append(role).append('\n');
            out.append("  THE RESPONSE:\n");
            out.append("    IMDSv1  ").append(said(Imds.fetchUrl(v1.base() + role))).append('\n');
            out.append("    IMDSv2  ").append(said(Imds.fetchUrl(v2.base() + role))).append('\n');
            out.append("  THE REQUESTS (a webhook tester: method and header chosen):\n");
            out.append("    PUT ").append(Imds.TOKEN_PATH).append('\n');
            out.append("        ").append(Imds.TTL_HEADER).append(": 60\n");
            out.append("    GET .../web-role\n");
            out.append("        ").append(Imds.TOKEN_HEADER).append(": <the token>\n");
            Imds.Fetched tok = Imds.webhookTester("PUT", v2.base() + Imds.TOKEN_PATH, Imds.TTL_HEADER, "60");
            Imds.Fetched got = Imds.webhookTester("GET", v2.base() + role, Imds.TOKEN_HEADER, tok.body());
            out.append("  THE RESPONSE:\n");
            out.append("    IMDSv2  PUT ").append(tok.status()).append(", a token of ").append(tok.body().length()).append(" characters\n");
            out.append("            GET ").append(said(got)).append('\n');
            out.append("  THE REQUESTS (the same, through a proxy):\n");
            out.append("    the proxy adds X-Forwarded-For: ").append(Imds.VISITOR).append('\n');
            Imds.Fetched p1 = Imds.throughProxy("GET", v1.base() + role, null, null);
            Imds.Fetched pTok = Imds.throughProxy("PUT", v2.base() + Imds.TOKEN_PATH, Imds.TTL_HEADER, "60");
            Imds.Fetched p2 = Imds.throughProxy("GET", v2.base() + role, Imds.TOKEN_HEADER, pTok.body());
            out.append("  THE RESPONSE:\n");
            out.append("    IMDSv1  GET ").append(said(p1)).append('\n');
            out.append("    IMDSv2  PUT ").append(pTok.status()).append(", no token\n");
            out.append("            GET ").append(said(p2)).append('\n');
        }
    }

    static String said(Imds.Fetched f) {
        return f.status() + (f.leaked() ? ", the role's credentials leaked" : ", nothing leaked");
    }

    private static void actFive(StringBuilder out, Cloud cloud) {
        out.append("FIVE. What the scanners and the emulator cannot tell.\n");
        out.append("  THE CALLS (on bank-audit, whose public access is blocked):\n");
        out.append("    PutBucketPolicy  Principal * may s3:GetObject\n");
        out.append("    PutBucketAcl     public-read\n");
        out.append("  THE ANSWER FROM MOTO:\n");
        Environment.tryToOpen(cloud, "bank-audit").forEach(l -> out.append("    ").append(l).append('\n'));
        out.append("    (AWS answers 403 AccessDenied: the block is enforced there)\n");
        out.append("  THE SCAN AFTERWARDS:\n");
        Scanner.scan(cloud).forEach(f -> out.append("    ").append(f).append('\n'));
        out.append("  WHAT IS LEFT FOR PEOPLE:\n");
        out.append("    replacement  a disk is encrypted only when it is made\n");
        out.append("    intent       a public website bucket looks like a leak:\n");
        out.append("                 it needs a written exception, an owner, a date\n");
        out.append("    drift        scan the files before deploy, the account after\n");
    }
}
