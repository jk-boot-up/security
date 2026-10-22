package com.jk.explore.security.cloudmisconf;

import java.nio.file.Path;
import java.util.List;

public class CloudMisconfigurationsDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static int remaining(java.util.Map<String, List<String>> map, IacScanners.Report report) {
        return (int) map.values().stream().filter(ids -> !IacScanners.catches(report.ids(), ids).isEmpty()).count();
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Venv venv = new Venv(Path.of("build"));
        try (Venv.Moto moto = new Venv.Moto(venv)) {
            Cloud cloud = new Cloud(moto.url());
            Environment.deployFirstVersion(cloud);
            List<Finding> findings = Scanner.scan(cloud);
            findings.forEach(f -> out.append(f).append('\n'));
            out.append("--- after the fixes\n");
            Environment.harden(cloud);
            Scanner.scan(cloud).forEach(f -> out.append(f).append('\n'));
            Path work = Path.of("build/tf");
            IacScanners.Report ck1 = IacScanners.checkov(venv, Terraform.write(work.resolve("first"), Terraform.FIRST));
            IacScanners.Report ck2 = IacScanners.checkov(venv, Terraform.write(work.resolve("fixed"), Terraform.FIXED));
            IacScanners.Report tv1 = IacScanners.trivy(work.resolve("first"));
            IacScanners.Report tv2 = IacScanners.trivy(work.resolve("fixed"));
            out.append("--- files before deploy\n");
            out.append("checkov: first version fails=" + (ck1.failed() > 0) + ", nine problems left in the fixed files=" + remaining(IacScanners.CHECKOV, ck2) + ", other advice left=" + (ck2.failed() > 0) + "\n");
            out.append("trivy:   first version fails=" + (tv1.failed() > 0) + ", nine problems left in the fixed files=" + remaining(IacScanners.TRIVY, tv2) + ", other advice left=" + (tv2.failed() > 0) + "\n");
            out.append("the one left is s3-no-encryption=" + (!IacScanners.catches(ck2.ids(), IacScanners.CHECKOV.get("s3-no-encryption")).isEmpty() && !IacScanners.catches(tv2.ids(), IacScanners.TRIVY.get("s3-no-encryption")).isEmpty())
                    + ": both tools ask for a customer-managed key, the rule accepts AES256\n");
            long both = IacScanners.CHECKOV.keySet().stream().filter(r -> !IacScanners.catches(ck1.ids(), IacScanners.CHECKOV.get(r)).isEmpty() && !IacScanners.catches(tv1.ids(), IacScanners.TRIVY.get(r)).isEmpty()).count();
            out.append("found by both tools: " + both + " of 9\n");
            for (String rule : IacScanners.CHECKOV.keySet().stream().sorted().toList()) {
                out.append(String.format("%-26s checkov=%-5s trivy=%s%n", rule,
                        !IacScanners.catches(ck1.ids(), IacScanners.CHECKOV.get(rule)).isEmpty(),
                        !IacScanners.catches(tv1.ids(), IacScanners.TRIVY.get(rule)).isEmpty()));
            }
        }
        out.append("--- the metadata endpoint\n");
        for (boolean v2 : new boolean[] {false, true}) {
            try (Imds imds = new Imds(v2)) {
                Imds.Fetched get = Imds.fetchUrl(imds.base() + Imds.ROLE_PATH);
                Imds.Fetched tok = Imds.webhookTester("PUT", imds.base() + "/latest/api/token", "X-aws-ec2-metadata-token-ttl-seconds", "60");
                Imds.Fetched full = Imds.webhookTester("GET", imds.base() + Imds.ROLE_PATH, "X-aws-ec2-metadata-token", tok.body());
                Imds.Fetched proxied = Imds.webhookTester("GET", imds.base() + Imds.ROLE_PATH, "X-Forwarded-For", "203.0.113.9");
                out.append("imds v" + (v2 ? 2 : 1) + ": url fetch " + get.status() + " leaked=" + get.leaked() + "; request forgery " + full.status() + " leaked=" + full.leaked() + "; behind a proxy " + proxied.status() + "\n");
            }
        }
        out.append("--- the bill\n");
        out.append("fixes that replace a resource: the server and the disks (2 of 9 rules)\n");
        out.append("a scanner sees the setting, not the intent: a public bucket for a website needs a written, dated exception\n");
        return out.toString();
    }
}
