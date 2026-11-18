package com.jk.explore.security.meshmtls;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Runs a bank's pods on a real kind cluster with Linkerd in six numbered acts. Each act prints its input and its result in separate, labelled
 * sections, and works out the values a reader would wonder about (an identity's parts, the number of pairs, a certificate's lifetime).
 */
public class ServiceMeshMtlsDemo {

    static final String MARKER = "ACCT-4417-transfer-9000";
    static final String CAPTURE = "tcp port 8080 or tcp port 4143";
    static final String HOSTILE = "' OR '1'='1";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** The first two parts of an identity (ServiceAccount and namespace), or the key as it is. */
    static String shortId(String identity) {
        int at = identity.indexOf(".serviceaccount.");
        return at < 0 ? identity : identity.substring(0, at);
    }

    static void table(StringBuilder out, Set<String> answered) {
        for (String row : Design.table(Lab.CALLERS, Lab.BANK, answered)) {
            line(out, "    " + row);
        }
    }

    static void score(StringBuilder out, Design.Diff d) {
        line(out, "    answered      " + d.answered() + " of " + d.total());
        line(out, "    not designed  " + d.unwanted().size() + " (answered, not in the design)");
        line(out, "    missing       " + d.missing().size() + " (in the design, refused)");
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        try (Lab lab = new Lab()) {
            Cluster c = lab.cluster;
            String call = Lab.PAYMENTS.url("/echo?msg=" + MARKER);

            line(out, "ONE. Before and after the proxies.");
            line(out, "  THE CALL (web calls payments; a capture runs on the node):");
            line(out, "    GET " + call);
            line(out, "  BEFORE THE PROXIES (" + lab.proxies() + " of 5 pods have one):");
            String[] code = new String[1];
            String wire = lab.sniffer.capture(0, CAPTURE, () -> code[0] = String.valueOf(c.http(Lab.WEB, call)));
            String seen = Wire.requestLine(wire, MARKER);
            line(out, "    answer             HTTP " + code[0]);
            line(out, "    request on wire    " + (seen.isEmpty() ? "none" : seen.replace(" HTTP/1.1", "")));
            line(out, "    account readable   " + yes(!seen.isEmpty()));
            lab.inject("bank", 4);
            line(out, "  AFTER THE PROXIES (" + lab.proxies() + " of 5 pods; legacy/batch left out):");
            wire = lab.sniffer.capture(0, CAPTURE, () -> code[0] = String.valueOf(c.http(Lab.WEB, call)));
            seen = Wire.requestLine(wire, MARKER);
            line(out, "    answer             HTTP " + code[0]);
            line(out, "    request on wire    " + (seen.isEmpty() ? "none" : seen.replace(" HTTP/1.1", "")));
            line(out, "    account readable   " + yes(!seen.isEmpty()));
            line(out, "    port 4143 used     " + yes(Wire.proxyPort(wire)) + " (the proxy's inbound port)");
            Map<String, Long> byServer = Metrics.byIdentity(c.proxyMetrics(Lab.PAYMENTS), "request_total", "inbound", "8080", "client_id");
            Map<String, Long> byClient = Metrics.byIdentity(c.proxyMetrics(Lab.WEB), "request_total", "outbound", "8080", "server_id");
            line(out, "  WHAT PAYMENTS' PROXY COUNTED (the client's certificate):");
            byServer.forEach((id, n) -> line(out, "    " + id + "  " + n));
            line(out, "  WHAT WEB'S PROXY COUNTED (the server's certificate):");
            byClient.forEach((id, n) -> line(out, "    " + id + "  " + n));
            line(out, "  THE RESULT:");
            line(out, "    both ends proved who they are: "
                    + yes(byServer.containsKey(Lab.WEB.identity()) && byClient.containsKey(Lab.PAYMENTS.identity())));

            line(out, "TWO. An identity from each ServiceAccount.");
            line(out, "  THE INPUT (the payments pod):");
            line(out, "    namespace           " + Lab.PAYMENTS.namespace());
            line(out, "    serviceAccountName  " + Lab.PAYMENTS.serviceAccount());
            line(out, "  HOW THE NAME IS MADE (the parts, joined by dots):");
            var parts = Lab.PAYMENTS.identityParts();
            line(out, "    ServiceAccount      " + parts.get(0));
            line(out, "    namespace           " + parts.get(1));
            line(out, "    fixed               " + parts.get(2));
            line(out, "    Linkerd namespace   " + parts.get(3));
            line(out, "    trust domain        " + parts.get(4));
            line(out, "  THE CERTIFICATES (linkerd identity, the subject):");
            int derived = 0;
            for (Service s : Lab.BANK) {
                CertInfo cert = c.identity(s);
                line(out, "    " + cert.subject());
                derived += cert.subject().equals(s.identity()) ? 1 : 0;
            }
            line(out, "    made by the rule above: " + derived + " of " + Lab.BANK.size());
            CertInfo firstCert = c.identity(Lab.PAYMENTS);

            line(out, "THREE. Mutual TLS alone does not decide who may call.");
            int pairs = Lab.CALLERS.size() * Lab.BANK.size() - Lab.BANK.size();
            line(out, "  THE CALLS (every caller to every bank service, no policy):");
            line(out, "    " + Lab.CALLERS.size() + " callers x " + Lab.BANK.size() + " services - " + Lab.BANK.size()
                    + " calls to itself = " + pairs + " pairs");
            line(out, "    the design allows 2: web -> payments, payments -> ledger");
            Set<String> open = lab.measure();
            line(out, "  WHO ANSWERED (row calls column):");
            table(out, open);
            Design.Diff openDiff = Lab.diff(open);
            line(out, "  THE RESULT:");
            score(out, openDiff);
            long fromBatch = openDiff.unwanted().stream().filter(p -> p.startsWith(Lab.BATCH.id())).count();
            line(out, "    from legacy/batch, no proxy   " + fromBatch + " of 4");
            Map<String, Long> plainIn = Metrics.byIdentity(c.proxyMetrics(Lab.LEDGER), "tcp_open_total", "inbound", "8080", "client_id");
            line(out, "    ledger let in no identity     " + yes(plainIn.containsKey(Metrics.NO_IDENTITY)));
            String ledgerIp = c.podIp(Lab.LEDGER);
            line(out, "    the node calls ledger         HTTP " + c.httpFromNode("http://" + ledgerIp + ":8080/hostname"));
            line(out, "    Linkerd's default policy      all-unauthenticated");

            line(out, "FOUR. A policy names who may call.");
            line(out, "  THE POLICY (a Server per service, one caller named):");
            line(out, "    web         Server, no caller named");
            line(out, "    statements  Server, no caller named");
            line(out, "    payments    Server + AuthorizationPolicy: web");
            line(out, "    ledger      Server + AuthorizationPolicy: payments");
            lab.applyDesignPolicy();
            Set<String> policed = lab.measure();
            line(out, "  WHO ANSWERED (row calls column):");
            table(out, policed);
            Design.Diff policedDiff = Lab.diff(policed);
            line(out, "  THE RESULT:");
            score(out, policedDiff);
            line(out, "    the measurement equals the design: " + yes(policedDiff.matches()));
            line(out, "  CALLS TO LEDGER, ONE BY ONE:");
            line(out, "    statements    HTTP " + c.http(Lab.STATEMENTS, Lab.LEDGER.url("/hostname")));
            line(out, "    web           HTTP " + c.http(Lab.WEB, Lab.LEDGER.url("/hostname")));
            line(out, "    legacy/batch  HTTP " + c.http(Lab.BATCH, Lab.LEDGER.url("/hostname")));
            line(out, "    payments      HTTP " + c.http(Lab.PAYMENTS, Lab.LEDGER.url("/hostname")));
            Map<String, Long> denied = Metrics.byIdentity(c.proxyMetrics(Lab.LEDGER), "inbound_http_authz_deny_total", "inbound", "8080", "client_id");
            line(out, "  LEDGER'S PROXY COUNTED REFUSALS (by the caller's identity):");
            denied.forEach((id, n) -> line(out, String.format("    %-16s %d", shortId(id), n)));

            line(out, "FIVE. What the mesh does not protect.");
            line(out, "  ROOT ON THE NODE:");
            line(out, "    the node calls ledger              HTTP " + c.httpFromNode("http://" + ledgerIp + ":8080/hostname"));
            int pid = c.podPid(Lab.PAYMENTS);
            String inside = lab.sniffer.capture(pid, "tcp port 8080", () -> c.http(Lab.WEB, call));
            line(out, "    inside payments, proxy to app      " + yes(!Wire.requestLine(inside, MARKER).isEmpty()));
            line(out, "  A POD OUTSIDE THE MESH:");
            String towardsLegacy = lab.sniffer.capture(0, CAPTURE, () -> code[0] = String.valueOf(c.http(Lab.WEB, Lab.BATCH.url("/echo?msg=" + MARKER))));
            line(out, "    web calls legacy/batch             HTTP " + code[0]);
            line(out, "    account readable on the wire       " + yes(!Wire.requestLine(towardsLegacy, MARKER).isEmpty()));
            line(out, "  AN ALLOWED CALLER, A HOSTILE REQUEST:");
            String hostile = "/echo?msg=%27%20OR%20%271%27=%271";
            int hostileCode = c.http(Lab.WEB, Lab.PAYMENTS.url(hostile));
            String echoed = c.body(Lab.WEB, Lab.PAYMENTS.url(hostile));
            line(out, "    web sends " + HOSTILE + " to payments  HTTP " + hostileCode);
            line(out, "    the string arrives intact          " + yes(echoed.contains(HOSTILE)));
            line(out, "  WHO MAY TAKE AN IDENTITY (calls to ledger):");
            Service impostor = new Service("bank", "impostor", "payments");
            c.apply(impostor.manifest());
            lab.settle("bank", 5);
            line(out, "    new pod in bank, runs as payments  HTTP " + c.http(impostor, Lab.LEDGER.url("/hostname")));
            Service shadow = new Service("shadow", "payments");
            c.must("annotate", "namespace", "shadow", "linkerd.io/inject=enabled", "--overwrite");
            c.apply(shadow.manifest());
            lab.settle("shadow", 1);
            line(out, "    payments in namespace shadow       HTTP " + c.http(shadow, Lab.LEDGER.url("/hostname")));
            line(out, "    its identity is " + shortId(shadow.identity()) + ", not " + shortId(Lab.PAYMENTS.identity()));
            line(out, "  WHO MAY CREATE PODS IN BANK:");
            Sh.Result webCan = c.kubectl("auth", "can-i", "create", "pods", "-n", "bank", "--as", "system:serviceaccount:bank:web");
            line(out, "    the web ServiceAccount             " + webCan.output().strip());
            c.must("-n", "bank", "create", "rolebinding", "dev-edit", "--clusterrole=edit", "--user=dev");
            Sh.Result devCan = c.kubectl("auth", "can-i", "create", "pods", "-n", "bank", "--as", "dev");
            line(out, "    a developer with the edit role     " + devCan.output().strip());

            line(out, "SIX. Certificate lifetime and renewal.");
            line(out, "  THE SETTING (linkerd install):");
            line(out, "    Linkerd's default lifetime   " + Linkerd.defaultIssuanceLifetime());
            line(out, "    this run                     " + Lab.ISSUANCE_LIFETIME);
            line(out, "  HOW THE LIFETIME IS READ (payments' certificate):");
            long span = firstCert.spanSeconds();
            long skew = CertInfo.SKEW.toSeconds();
            line(out, "    Not After - Not Before       " + span + " s");
            line(out, "    minus the clock skew, twice  " + span + " - 2 x " + skew + " = " + firstCert.lifetime().toSeconds()
                    + " s = " + firstCert.lifetime().toMinutes() + " minutes");
            CertInfo anchor = CertInfo.fromPem(c.must("-n", "linkerd", "get", "configmap", "linkerd-identity-trust-roots", "-o", "jsonpath={.data.ca-bundle\\.crt}"));
            line(out, "    the trust anchor, valid for  " + Duration.between(anchor.notBefore(), anchor.notAfter()).toDays() + " days");
            CertInfo later = firstCert;
            for (int i = 0; i < 60 && !later.notAfter().isAfter(firstCert.notAfter()); i++) {
                Thread.sleep(15_000);
                later = c.identity(Lab.PAYMENTS);
            }
            line(out, "  AFTER THE RENEWAL (checked every 15 s):");
            line(out, "    a newer certificate          " + yes(later.notAfter().isAfter(firstCert.notAfter())));
            line(out, "    the same name                " + yes(later.subject().equals(firstCert.subject())));
            line(out, "    the pod restarted            " + yes(lab.restarts(Lab.PAYMENTS) > 0));
            line(out, "    payments calls ledger        HTTP " + c.http(Lab.PAYMENTS, Lab.LEDGER.url("/hostname")));
        }
        return out.toString();
    }
}
