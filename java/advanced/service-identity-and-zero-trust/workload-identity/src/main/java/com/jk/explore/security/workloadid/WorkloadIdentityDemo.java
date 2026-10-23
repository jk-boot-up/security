package com.jk.explore.security.workloadid;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A bank's ledger is called by payments and reporting. Before: one shared secret copied into three services. After: SPIRE gives each
 * workload a SPIFFE ID, chosen by a registration entry and the operating system user it runs as, and an X.509 SVID that lives 20 seconds.
 * Every number printed below was measured against a real SPIRE server and agent.
 */
public final class WorkloadIdentityDemo {

    private static final String LEDGER_ID = "spiffe://bank.example/ledger";
    private static final String PAYMENTS_ID = "spiffe://bank.example/payments";
    private static final int ATTACKER = Lab.UIDS.get("attacker");
    private static final int PAYMENTS = Lab.UIDS.get("payments");
    private static final int REPORTING = Lab.UIDS.get("reporting");
    private static final int LEDGER = Lab.UIDS.get("ledger");

    private WorkloadIdentityDemo() {
    }

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        // a fresh random secret every run, written only under build/ and never printed
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        String secret = HexFormat.of().formatHex(bytes);
        try (LegacyLedger legacy = new LegacyLedger(secret)) {
            String leakedStatus;
            beforeSecret(out, legacy, secret);
            try (Lab lab = Lab.start()) {
                spire(out, lab);
                services(out, lab);
                rotation(out, lab);
                leakedStatus = stolen(out, lab, legacy, secret);
                noEntry(out, lab);
            }
            bill(out, legacy, secret, leakedStatus);
        }
        return out.toString();
    }

    // ---- 1. the before

    private static void beforeSecret(StringBuilder out, LegacyLedger legacy, String secret) throws IOException, InterruptedException {
        line(out, "--- before: one shared secret in three services");
        Path configs = Path.of("build", "legacy");
        Lab.deleteTree(configs);
        Files.createDirectories(configs);
        int holders = 0;
        for (String service : List.of("payments", "reporting", "fraud-engine")) {
            Files.writeString(configs.resolve(service + ".conf"), "ledger.api.key=" + secret + "\n");
            holders++;
        }
        line(out, "the same secret is copied into the configuration of payments, reporting and fraud-engine: " + holders + " of 3 hold it");
        line(out, "payments (says payments), POST /transfers: " + legacy("POST", "/transfers", secret, "payments", legacy));
        line(out, "reporting (says reporting), GET /statements: " + legacy("GET", "/statements", secret, "reporting", legacy));
        line(out, "reporting (says reporting), POST /transfers: " + legacy("POST", "/transfers", secret, "reporting", legacy));
        line(out, "reporting (says payments), POST /transfers: " + legacy("POST", "/transfers", secret, "payments", legacy)
                + " (the ledger takes the caller's word)");
        String leaked = Files.readString(configs.resolve("fraud-engine.conf")).trim().substring("ledger.api.key=".length());
        line(out, "the fraud-engine's configuration leaks; an attacker outside the bank who says payments, POST /transfers: "
                + legacy("POST", "/transfers", leaked, "payments", legacy));
        line(out, "the same attacker saying reporting, GET /statements: " + legacy("GET", "/statements", leaked, "reporting", legacy));
        line(out, "the attacker is every service: it holds the one thing each service holds");
        line(out, "a wrong key: " + legacy("POST", "/transfers", "wrong", "payments", legacy));
    }

    private static int legacy(String method, String path, String key, String claim, LegacyLedger ledger) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + ledger.port() + path))
                .method(method, method.equals("POST") ? HttpRequest.BodyPublishers.ofString("{}") : HttpRequest.BodyPublishers.noBody())
                .header("X-Api-Key", key).header("X-Service", claim).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    // ---- 2. SPIRE decides who a workload is

    private static void spire(StringBuilder out, Lab lab) throws IOException, InterruptedException {
        line(out, "--- SPIRE decides who a workload is");
        line(out, "SPIRE server and agent " + Lab.SPIRE_VERSION + " run in one Docker container (linux arm64; SPIRE publishes no macOS build), trust domain "
                + Lab.DOMAIN);
        line(out, "registration entries: " + lab.entryCount() + ", each chosen by the operating system user the process runs as, none by anything it says");
        for (String name : List.of("ledger", "payments", "reporting")) {
            String who = lab.workload(Lab.UIDS.get(name), "whoami");
            line(out, "the Java program run as user " + Lab.UIDS.get(name) + " (" + name + "): " + field(who, "id"));
        }
        String liar = lab.workloadClaiming(REPORTING, PAYMENTS_ID, "whoami");
        line(out, "the same program run as the reporting user, with the claim \"I am " + PAYMENTS_ID + "\" in its environment: " + field(liar, "id"));
        line(out, "an SVID that was just issued has at most " + Lab.SVID_TTL_SECONDS + " seconds left: "
                + yes(Long.parseLong(field(liar, "remaining")) <= Lab.SVID_TTL_SECONDS) + " (the certificate's own dates span " + field(liar, "span")
                + " seconds: SPIRE starts them 10 seconds early to forgive clock skew)");
    }

    // ---- 3. two Java services, mutual verification

    private static void services(StringBuilder out, Lab lab) throws IOException, InterruptedException {
        line(out, "--- two Java services, each checking the other's SPIFFE ID");
        lab.spawn(LEDGER, "ledger", "ledger", "ledger");
        line(out, "payments, POST /transfers: " + lab.workload(PAYMENTS, "call", "ledger", LEDGER_ID, "POST", "/transfers"));
        line(out, "payments, GET /statements: " + lab.workload(PAYMENTS, "call", "ledger", LEDGER_ID, "GET", "/statements"));
        line(out, "reporting, GET /statements: " + lab.workload(REPORTING, "call", "ledger", LEDGER_ID, "GET", "/statements"));
        line(out, "reporting, POST /transfers: " + lab.workload(REPORTING, "call", "ledger", LEDGER_ID, "POST", "/transfers"));
        TreeSet<String> callers = new TreeSet<>();
        for (String entry : lab.audit("ledger")) {
            callers.add(entry.split(" ")[0]);
        }
        line(out, "the ledger's audit names " + lab.audit("ledger").size() + " requests by the identity in the certificate: " + String.join(", ", callers));

        lab.spawn(REPORTING, "fake", "ledger", "fake");
        line(out, "a server run by the reporting workload, called by payments as if it were the ledger: "
                + lab.workload(PAYMENTS, "call", "fake", LEDGER_ID, "POST", "/transfers") + " (payments expects " + LEDGER_ID + ")");
        int before = lab.audit("ledger").size();
        lab.root("keytool", "-genkeypair", "-alias", "forged", "-keyalg", "EC", "-groupname", "secp256r1", "-dname", "CN=payments", "-ext",
                "san=uri:" + PAYMENTS_ID, "-validity", "1", "-keystore", "/work/forged.p12", "-storetype", "PKCS12",
                "-storepass", "lab-password", "-keypass", "lab-password");
        lab.root("chmod", "a+r", "/work/forged.p12");
        line(out, "a certificate made by the attacker that names itself " + PAYMENTS_ID + ": "
                + lab.workload(ATTACKER, "forged", "/work/forged.p12", "/work/bundle.pem", "ledger", "/transfers")
                + ", requests that reached the ledger's code: " + (lab.audit("ledger").size() - before));
    }

    // ---- 4. rotation

    private static void rotation(StringBuilder out, Lab lab) throws IOException, InterruptedException {
        line(out, "--- rotation: an SVID that lives " + Lab.SVID_TTL_SECONDS + " seconds");
        String result = lab.workload(PAYMENTS, "loop", "ledger", LEDGER_ID, "50");
        int calls = Integer.parseInt(field(result, "calls"));
        line(out, "payments called the ledger on new connections for 50 seconds: at least 100 calls " + yes(calls >= 100)
                + ", failed " + field(result, "failed"));
        line(out, "payments' own SVID was replaced at least 3 times " + yes(Integer.parseInt(field(result, "own")) >= 3)
                + "; the ledger's, as seen by payments, at least 3 times " + yes(Integer.parseInt(field(result, "server")) >= 3));
        line(out, "the services contain no renewal code: the Workload API client swaps the SVID and the TLS context reads it at each handshake");
    }

    // ---- 5. a stolen SVID

    private static String stolen(StringBuilder out, Lab lab, LegacyLedger legacy, String secret) throws IOException, InterruptedException {
        line(out, "--- a stolen SVID");
        lab.root("mkdir", "-p", "/work/payments-disk");
        lab.root("chmod", "777", "/work/payments-disk");
        lab.agentCli(PAYMENTS, "api", "fetch", "x509", "-write", "/work/payments-disk");
        long copied = System.currentTimeMillis();
        lab.root("sh", "-c", "mkdir -p /work/attacker && cp /work/payments-disk/* /work/attacker/ && chmod -R a+rX /work/attacker");
        line(out, "the files an SVID helper leaves on payments' disk (certificate, private key, bundle) are copied by an attacker running as another user");
        line(out, "the attacker, POST /transfers: " + lab.workload(ATTACKER, "stolen", "/work/attacker", "ledger", LEDGER_ID, "POST", "/transfers"));
        List<String> audit = lab.audit("ledger");
        line(out, "the ledger's audit names that call: " + audit.get(audit.size() - 1).split(" ")[0] + " (a copy is as good as the original while it lives)");
        long wait = copied + (Lab.SVID_TTL_SECONDS + 1) * 1000L - System.currentTimeMillis();
        Thread.sleep(Math.max(wait, 0));
        line(out, "after the SVID's lifetime (" + Lab.SVID_TTL_SECONDS + " seconds), the attacker's call: " + lab.workload(ATTACKER, "stolen", "/work/attacker", "ledger", LEDGER_ID,
                "POST", "/transfers"));
        line(out, "payments itself, at the same moment: " + lab.workload(PAYMENTS, "call", "ledger", LEDGER_ID, "POST", "/transfers"));
        String leaked = Files.readString(Path.of("build", "legacy", "fraud-engine.conf")).trim().substring("ledger.api.key=".length());
        int leakedStatus = legacy("POST", "/transfers", leaked, "payments", legacy);
        line(out, "the leaked shared secret, at the same moment: " + leakedStatus);
        return Integer.toString(leakedStatus);
    }

    // ---- 6. no entry

    private static void noEntry(StringBuilder out, Lab lab) throws IOException, InterruptedException {
        line(out, "--- a workload that matches no entry");
        int before = lab.audit("ledger").size();
        line(out, "a process run as the attacker's user, saying it is " + PAYMENTS_ID + ": " + shown(lab.workloadWithoutEntry(ATTACKER, PAYMENTS_ID, "whoami")));
        line(out, "the same process calling the ledger: " + shown(lab.workloadWithoutEntry(ATTACKER, PAYMENTS_ID, "call", "ledger", LEDGER_ID, "POST", "/transfers"))
                + ", requests that reached the ledger's code: " + (lab.audit("ledger").size() - before));
        lab.deleteEntry("payments");
        long end = System.currentTimeMillis() + 40_000;
        while (System.currentTimeMillis() < end && lab.agentCli(PAYMENTS, "api", "fetch", "x509").ok()) {
            Thread.sleep(500);
        }
        line(out, "the payments entry is deleted; a new payments process, within 40 seconds: " + shown(lab.workloadWithoutEntry(PAYMENTS, null, "whoami")));
        line(out, "reporting, unaffected, GET /statements: " + lab.workload(REPORTING, "call", "ledger", LEDGER_ID, "GET", "/statements"));
    }

    // ---- 7. the bill

    private static void bill(StringBuilder out, LegacyLedger legacy, String secret, String leakedStatus) throws IOException, InterruptedException {
        line(out, "--- the bill");
        String fresh = HexFormat.of().formatHex(new SecureRandom().generateSeed(24));
        legacy.rotateSecret(fresh);
        int broken = 0;
        for (String service : List.of("payments", "reporting", "fraud-engine")) {
            if (legacy("POST", "/transfers", secret, service, legacy) == 401) {
                broken++;
            }
        }
        line(out, "evicting the attacker by changing the shared secret: the leaked copy " + legacy("POST", "/transfers", secret, "payments", legacy)
                + ", and " + broken + " of 3 services fail until each is given the new secret");
        line(out, "evicting a stolen SVID changed nothing in any service: it expired by itself (the leaked secret was still " + leakedStatus + " until it was changed)");
        line(out, "the identity is only as strong as the selector: any process that runs as the payments user on that node is payments");
        line(out, "a short lifetime is the revocation that always works, and it costs a SPIRE server and agent that must stay up to renew");
    }

    // ---- helpers

    private static String field(String result, String name) {
        Matcher m = Pattern.compile(name + "=(\\S+)").matcher(result);
        return m.find() ? m.group(1) : result;
    }

    private static String shown(String result) {
        return result.equals("no-svid") ? "no SVID issued" : result;
    }

    private static String yes(boolean value) {
        return value ? "yes" : "no";
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }
}
