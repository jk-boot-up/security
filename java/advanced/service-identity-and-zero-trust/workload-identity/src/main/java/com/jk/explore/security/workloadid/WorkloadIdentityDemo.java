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
        line(out, "ONE. One shared secret in three services.");
        Path configs = Path.of("build", "legacy");
        Lab.deleteTree(configs);
        Files.createDirectories(configs);
        int holders = 0;
        for (String service : List.of("payments", "reporting", "fraud-engine")) {
            Files.writeString(configs.resolve(service + ".conf"), "ledger.api.key=" + secret + "\n");
            holders++;
        }
        line(out, "  THE SECRET (24 random bytes, never printed):");
        line(out, "    copied into payments, reporting, fraud-engine: " + holders + " of 3");
        line(out, "  THE LEDGER'S RULES:");
        for (Policy.Rule rule : Policy.RULES) {
            line(out, String.format("    %-33s %s %s", rule.callerId(), rule.method(), rule.path()));
        }
        line(out, "  THE REQUESTS (the right key; X-Service says who calls):");
        line(out, "    caller     says       request          answer");
        request(out, "reporting", "reporting", "GET", "/statements", legacy("GET", "/statements", secret, "reporting", legacy));
        request(out, "payments", "payments", "POST", "/transfers", legacy("POST", "/transfers", secret, "payments", legacy));
        request(out, "reporting", "reporting", "POST", "/transfers", legacy("POST", "/transfers", secret, "reporting", legacy));
        request(out, "reporting", "payments", "POST", "/transfers", legacy("POST", "/transfers", secret, "payments", legacy));
        line(out, "    the ledger takes the caller's word for who it is");
        String leaked = Files.readString(configs.resolve("fraud-engine.conf")).trim().substring("ledger.api.key=".length());
        line(out, "  THE LEAK (fraud-engine's configuration, outside the bank):");
        line(out, "    caller     says       request          answer");
        request(out, "attacker", "payments", "POST", "/transfers", legacy("POST", "/transfers", leaked, "payments", legacy));
        request(out, "attacker", "reporting", "GET", "/statements", legacy("GET", "/statements", leaked, "reporting", legacy));
        request(out, "wrong key", "payments", "POST", "/transfers", legacy("POST", "/transfers", "wrong", "payments", legacy));
        line(out, "    with the one secret, the attacker is every service");
    }

    private static void request(StringBuilder out, String caller, String says, String method, String path, Object answer) {
        line(out, String.format("    %-10s %-10s %-16s %s", caller, says, method + " " + path, answer));
    }

    private static int legacy(String method, String path, String key, String claim, LegacyLedger ledger) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + ledger.port() + path))
                .method(method, method.equals("POST") ? HttpRequest.BodyPublishers.ofString("{}") : HttpRequest.BodyPublishers.noBody())
                .header("X-Api-Key", key).header("X-Service", claim).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    // ---- 2. SPIRE decides who a workload is

    private static void spire(StringBuilder out, Lab lab) throws IOException, InterruptedException {
        line(out, "TWO. SPIRE decides who a workload is.");
        line(out, "  THE SETUP (server and agent in one Linux container):");
        line(out, "    SPIRE          " + Lab.SPIRE_VERSION + " (linux arm64; no macOS build exists)");
        line(out, "    trust domain   " + Lab.DOMAIN);
        line(out, "    entries        " + lab.entryCount() + ", chosen by the user a process runs as");
        line(out, "  THE REGISTRATION ENTRIES (selector -> SPIFFE ID):");
        for (String name : List.of("ledger", "payments", "reporting")) {
            line(out, "    unix:uid:" + Lab.UIDS.get(name) + "  ->  spiffe://" + Lab.DOMAIN + "/" + name);
        }
        line(out, "  THE SAME PROGRAM, RUN AS EACH USER (the SVID it got):");
        for (String name : List.of("ledger", "payments", "reporting")) {
            String who = lab.workload(Lab.UIDS.get(name), "whoami");
            line(out, String.format("    %-26s %s", "user " + Lab.UIDS.get(name), field(who, "id")));
        }
        String liar = lab.workloadClaiming(REPORTING, PAYMENTS_ID, "whoami");
        line(out, String.format("    %-26s %s", "user " + REPORTING + ", says payments", field(liar, "id")));
        SpiffeIds.Parts parts = SpiffeIds.parts(field(liar, "id"));
        line(out, "  ONE SPIFFE ID, PART BY PART:");
        line(out, "    scheme         " + SpiffeIds.SCHEME);
        line(out, "    trust domain   " + parts.trustDomain());
        line(out, "    path           " + parts.path());
        long span = Long.parseLong(field(liar, "span"));
        line(out, "  HOW LONG AN SVID LIVES (from its own dates):");
        line(out, "    Not After - Not Before     " + span + " s");
        line(out, "    minus " + Lab.BACKDATE_SECONDS + " s back-dating     " + span + " - " + Lab.BACKDATE_SECONDS + " = "
                + (span - Lab.BACKDATE_SECONDS) + " s, the TTL");
        line(out, "    left when just issued      at most " + Lab.SVID_TTL_SECONDS + " s: "
                + yes(Long.parseLong(field(liar, "remaining")) <= Lab.SVID_TTL_SECONDS));
    }

    // ---- 3. two Java services, mutual verification

    private static void services(StringBuilder out, Lab lab) throws IOException, InterruptedException {
        line(out, "THREE. Two Java services check each other's SPIFFE ID.");
        lab.spawn(LEDGER, "ledger", "ledger", "ledger");
        line(out, "  THE REQUESTS (mutual TLS; the ID comes from the certificate):");
        line(out, "    caller     request          answer");
        call(out, "payments", "POST", "/transfers", lab.workload(PAYMENTS, "call", "ledger", LEDGER_ID, "POST", "/transfers"));
        call(out, "payments", "GET", "/statements", lab.workload(PAYMENTS, "call", "ledger", LEDGER_ID, "GET", "/statements"));
        call(out, "reporting", "GET", "/statements", lab.workload(REPORTING, "call", "ledger", LEDGER_ID, "GET", "/statements"));
        call(out, "reporting", "POST", "/transfers", lab.workload(REPORTING, "call", "ledger", LEDGER_ID, "POST", "/transfers"));
        TreeSet<String> callers = new TreeSet<>();
        for (String entry : lab.audit("ledger")) {
            callers.add(entry.split(" ")[0]);
        }
        line(out, "  THE LEDGER'S AUDIT (" + lab.audit("ledger").size() + " requests, named by the certificate):");
        for (String caller : callers) {
            line(out, "    " + caller);
        }
        line(out, "  TWO IMPOSTORS:");
        lab.spawn(REPORTING, "fake", "ledger", "fake");
        line(out, "    a server run as reporting, called as the ledger: "
                + lab.workload(PAYMENTS, "call", "fake", LEDGER_ID, "POST", "/transfers"));
        line(out, "      payments expects " + LEDGER_ID);
        int before = lab.audit("ledger").size();
        lab.root("keytool", "-genkeypair", "-alias", "forged", "-keyalg", "EC", "-groupname", "secp256r1", "-dname", "CN=payments", "-ext",
                "san=uri:" + PAYMENTS_ID, "-validity", "1", "-keystore", "/work/forged.p12", "-storetype", "PKCS12",
                "-storepass", "lab-password", "-keypass", "lab-password");
        lab.root("chmod", "a+r", "/work/forged.p12");
        line(out, "    a self-made certificate naming payments: "
                + lab.workload(ATTACKER, "forged", "/work/forged.p12", "/work/bundle.pem", "ledger", "/transfers"));
        line(out, "      requests that reached the ledger's code: " + (lab.audit("ledger").size() - before));
    }

    private static void call(StringBuilder out, String caller, String method, String path, String answer) {
        line(out, String.format("    %-10s %-16s %s", caller, method + " " + path, answer));
    }

    // ---- 4. rotation

    private static void rotation(StringBuilder out, Lab lab) throws IOException, InterruptedException {
        line(out, "FOUR. Rotation: an SVID that lives " + Lab.SVID_TTL_SECONDS + " seconds.");
        line(out, "  THE TEST (payments calls the ledger, new connection each):");
        line(out, "    for            50 s, about 4 calls a second");
        line(out, "    SVID lifetime  " + Lab.SVID_TTL_SECONDS + " s, so 50 / " + Lab.SVID_TTL_SECONDS + " = 2.5 lifetimes");
        String result = lab.workload(PAYMENTS, "loop", "ledger", LEDGER_ID, "50");
        int calls = Integer.parseInt(field(result, "calls"));
        line(out, "  THE RESULT:");
        line(out, "    calls, at least 100        " + yes(calls >= 100));
        line(out, "    calls that failed          " + field(result, "failed"));
        line(out, "    payments' SVID replaced    at least 3 times: " + yes(Integer.parseInt(field(result, "own")) >= 3));
        line(out, "    the ledger's, as seen      at least 3 times: " + yes(Integer.parseInt(field(result, "server")) >= 3));
        line(out, "    renewal code in services   none");
        line(out, "    the Workload API client swaps the SVID; each TLS");
        line(out, "    handshake reads the current one");
    }

    // ---- 5. a stolen SVID

    private static String stolen(StringBuilder out, Lab lab, LegacyLedger legacy, String secret) throws IOException, InterruptedException {
        line(out, "FIVE. A stolen SVID.");
        lab.root("mkdir", "-p", "/work/payments-disk");
        lab.root("chmod", "777", "/work/payments-disk");
        lab.agentCli(PAYMENTS, "api", "fetch", "x509", "-write", "/work/payments-disk");
        long copied = System.currentTimeMillis();
        lab.root("sh", "-c", "mkdir -p /work/attacker && cp /work/payments-disk/* /work/attacker/ && chmod -R a+rX /work/attacker");
        line(out, "  THE THEFT (files an SVID helper left on payments' disk):");
        line(out, "    certificate, private key, bundle: copied by another user");
        line(out, "  THE ATTACKER'S CALLS (POST /transfers, with the copy):");
        line(out, "    at once            " + lab.workload(ATTACKER, "stolen", "/work/attacker", "ledger", LEDGER_ID, "POST", "/transfers"));
        List<String> audit = lab.audit("ledger");
        line(out, "    the audit names    " + audit.get(audit.size() - 1).split(" ")[0]);
        long wait = copied + (Lab.SVID_TTL_SECONDS + 1) * 1000L - System.currentTimeMillis();
        Thread.sleep(Math.max(wait, 0));
        line(out, "    after " + (Lab.SVID_TTL_SECONDS + 1) + " s (> " + Lab.SVID_TTL_SECONDS + ")  "
                + lab.workload(ATTACKER, "stolen", "/work/attacker", "ledger", LEDGER_ID, "POST", "/transfers"));
        line(out, "  AT THE SAME MOMENT:");
        line(out, "    payments itself            " + lab.workload(PAYMENTS, "call", "ledger", LEDGER_ID, "POST", "/transfers"));
        String leaked = Files.readString(Path.of("build", "legacy", "fraud-engine.conf")).trim().substring("ledger.api.key=".length());
        int leakedStatus = legacy("POST", "/transfers", leaked, "payments", legacy);
        line(out, "    the leaked shared secret   " + leakedStatus);
        return Integer.toString(leakedStatus);
    }

    // ---- 6. no entry

    private static void noEntry(StringBuilder out, Lab lab) throws IOException, InterruptedException {
        line(out, "SIX. A workload that matches no entry.");
        int before = lab.audit("ledger").size();
        line(out, "  THE ATTACKER'S OWN USER (" + ATTACKER + "), SAYING IT IS PAYMENTS:");
        line(out, "    asks for an SVID            " + shown(lab.workloadWithoutEntry(ATTACKER, PAYMENTS_ID, "whoami")));
        line(out, "    calls the ledger            " + shown(lab.workloadWithoutEntry(ATTACKER, PAYMENTS_ID, "call", "ledger", LEDGER_ID, "POST", "/transfers")));
        line(out, "    reached the ledger's code   " + (lab.audit("ledger").size() - before));
        lab.deleteEntry("payments");
        long end = System.currentTimeMillis() + 40_000;
        while (System.currentTimeMillis() < end && lab.agentCli(PAYMENTS, "api", "fetch", "x509").ok()) {
            Thread.sleep(500);
        }
        line(out, "  THE PAYMENTS ENTRY IS DELETED (checked within 40 s):");
        line(out, "    a new payments process      " + shown(lab.workloadWithoutEntry(PAYMENTS, null, "whoami")));
        line(out, "    reporting, GET /statements  " + lab.workload(REPORTING, "call", "ledger", LEDGER_ID, "GET", "/statements"));
    }

    // ---- 7. evicting the attacker

    private static void bill(StringBuilder out, LegacyLedger legacy, String secret, String leakedStatus) throws IOException, InterruptedException {
        line(out, "SEVEN. Evicting the attacker.");
        String fresh = HexFormat.of().formatHex(new SecureRandom().generateSeed(24));
        legacy.rotateSecret(fresh);
        int broken = 0;
        for (String service : List.of("payments", "reporting", "fraud-engine")) {
            if (legacy("POST", "/transfers", secret, service, legacy) == 401) {
                broken++;
            }
        }
        line(out, "  THE SHARED SECRET IS CHANGED:");
        line(out, "    the leaked copy        " + legacy("POST", "/transfers", secret, "payments", legacy));
        line(out, "    services that fail     " + broken + " of 3, until each gets the new one");
        line(out, "  THE STOLEN SVID:");
        line(out, "    services changed       0: it expired by itself");
        line(out, "    the leaked secret      " + leakedStatus + " until it was changed");
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
