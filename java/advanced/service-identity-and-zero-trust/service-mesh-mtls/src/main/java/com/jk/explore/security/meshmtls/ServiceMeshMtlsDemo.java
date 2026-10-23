package com.jk.explore.security.meshmtls;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ServiceMeshMtlsDemo {

    private static final String MARKER = "ACCT-4417-transfer-9000";
    private static final Pattern PROXY_PORT = Pattern.compile("\\.4143[ :]");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    static String line(Design.Diff d) {
        return String.format("%d of %d pairs answered, %d unwanted, %d missing", d.answered(), d.total(), d.unwanted().size(), d.missing().size());
    }

    static String counts(Map<String, Long> m) {
        return m.isEmpty() ? "nothing" : m.toString().replaceAll("^\\{|}$", "");
    }

    static String minutes(Duration d) {
        return d.toMinutes() + " minutes";
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        try (Lab lab = new Lab()) {
            Cluster c = lab.cluster;
            String call = Lab.PAYMENTS.url("/echo?msg=" + MARKER);

            out.append("--- before the proxies: a call between two pods\n");
            out.append("Linkerd's control plane is installed, the bank namespace is not injected yet\n");
            out.append("pods with a proxy: " + lab.proxies() + " of 5\n");
            String[] plain = new String[1];
            String wire = lab.sniffer.capture(0, "tcp port 8080 or tcp port 4143", () -> plain[0] = String.valueOf(c.http(Lab.WEB, call)));
            out.append("web calls payments: HTTP " + plain[0] + "\n");
            out.append("the account number can be read from the wire: " + yes(wire.contains(MARKER)) + "\n");

            out.append("--- inject the proxies\n");
            lab.inject("bank", 4);
            out.append("pods with a proxy: " + lab.proxies() + " of 5 (the legacy namespace was left out)\n");
            String[] meshed = new String[1];
            wire = lab.sniffer.capture(0, "tcp port 8080 or tcp port 4143", () -> meshed[0] = String.valueOf(c.http(Lab.WEB, call)));
            out.append("web calls payments: HTTP " + meshed[0] + "\n");
            out.append("the account number can be read from the wire: " + yes(wire.contains(MARKER)) + "\n");
            out.append("packets seen on the proxy port 4143: " + yes(PROXY_PORT.matcher(wire).find()) + "\n");
            Map<String, Long> seenByServer = Metrics.byIdentity(c.proxyMetrics(Lab.PAYMENTS), "request_total", "inbound", "8080", "client_id");
            Map<String, Long> seenByClient = Metrics.byIdentity(c.proxyMetrics(Lab.WEB), "request_total", "outbound", "8080", "server_id");
            out.append("payments' proxy counted requests by the client's certificate: " + counts(seenByServer) + "\n");
            out.append("web's proxy counted requests by the server's certificate: " + counts(seenByClient) + "\n");
            out.append("both ends proved who they are: " + yes(seenByServer.containsKey(Lab.WEB.identity()) && seenByClient.containsKey(Lab.PAYMENTS.identity())) + "\n");

            out.append("--- an identity for each workload, taken from its ServiceAccount\n");
            boolean derived = true;
            for (Service s : Lab.BANK) {
                CertInfo cert = c.identity(s);
                out.append(s.id() + " holds a certificate for " + cert.subject() + "\n");
                derived &= cert.subject().equals(s.identity());
            }
            out.append("each name is the ServiceAccount, the namespace and the trust domain: " + yes(derived) + "\n");
            CertInfo firstCert = c.identity(Lab.PAYMENTS);

            out.append("--- mutual TLS alone does not decide who may call\n");
            Design.Diff open = lab.diff();
            out.append("no policy: " + line(open) + "\n");
            long fromBatch = open.unwanted().stream().filter(p -> p.startsWith(Lab.BATCH.id())).count();
            out.append("pairs answered for the pod outside the mesh: " + fromBatch + " of 4\n");
            Map<String, Long> plainIn = Metrics.byIdentity(c.proxyMetrics(Lab.LEDGER), "tcp_open_total", "inbound", "8080", "client_id");
            out.append("ledger's proxy accepted connections without any identity: " + yes(plainIn.containsKey(Metrics.NO_IDENTITY)) + "\n");
            String ledgerIp = c.podIp(Lab.LEDGER);
            out.append("the node itself, with no proxy and no identity, calls ledger: HTTP " + c.httpFromNode("http://" + ledgerIp + ":8080/hostname") + "\n");
            out.append("the default inbound policy of Linkerd 'all-unauthenticated' lets every caller in\n");

            out.append("--- a policy names who may call\n");
            lab.applyDesignPolicy();
            Design.Diff policed = lab.diff();
            out.append("a Server for each service, an AuthorizationPolicy naming one identity: " + line(policed) + "\n");
            out.append("the measurement equals the design: " + yes(policed.matches()) + "\n");
            out.append("statements calls ledger: HTTP " + c.http(Lab.STATEMENTS, Lab.LEDGER.url("/hostname")) + "\n");
            out.append("web calls ledger directly: HTTP " + c.http(Lab.WEB, Lab.LEDGER.url("/hostname")) + "\n");
            out.append("the pod outside the mesh calls ledger: HTTP " + c.http(Lab.BATCH, Lab.LEDGER.url("/hostname")) + "\n");
            out.append("payments calls ledger: HTTP " + c.http(Lab.PAYMENTS, Lab.LEDGER.url("/hostname")) + "\n");
            Map<String, Long> denied = Metrics.byIdentity(c.proxyMetrics(Lab.LEDGER), "inbound_http_authz_deny_total", "inbound", "8080", "client_id");
            out.append("ledger's proxy counted refusals by caller: " + counts(denied) + "\n");

            out.append("--- what the mesh does not protect\n");
            out.append("the node itself calls ledger: HTTP " + c.httpFromNode("http://" + ledgerIp + ":8080/hostname") + "\n");
            int pid = c.podPid(Lab.PAYMENTS);
            String inside = lab.sniffer.capture(pid, "tcp port 8080", () -> c.http(Lab.WEB, call));
            out.append("root on the node reads the account number inside the payments pod, between proxy and app: " + yes(inside.contains(MARKER)) + "\n");

            String towardsLegacy = lab.sniffer.capture(0, "tcp port 8080 or tcp port 4143", () -> plain[0] = String.valueOf(c.http(Lab.WEB, Lab.BATCH.url("/echo?msg=" + MARKER))));
            out.append("web calls the pod outside the mesh: HTTP " + plain[0] + ", the account number can be read from the wire: " + yes(towardsLegacy.contains(MARKER)) + "\n");

            String hostile = "/echo?msg=%27%20OR%20%271%27=%271";
            int hostileCode = c.http(Lab.WEB, Lab.PAYMENTS.url(hostile));
            String echoed = c.body(Lab.WEB, Lab.PAYMENTS.url(hostile));
            out.append("web, allowed, sends an injection string to payments: HTTP " + hostileCode + ", the string arrives intact: " + yes(echoed.contains("' OR '1'='1")) + "\n");

            Service impostor = new Service("bank", "impostor", "payments");
            c.apply(impostor.manifest());
            lab.settle("bank", 5);
            out.append("a new pod in bank that runs as the payments ServiceAccount calls ledger: HTTP " + c.http(impostor, Lab.LEDGER.url("/hostname")) + "\n");
            Service shadow = new Service("shadow", "payments");
            c.must("annotate", "namespace", "shadow", "linkerd.io/inject=enabled", "--overwrite");
            c.apply(shadow.manifest());
            lab.settle("shadow", 1);
            out.append("a ServiceAccount also named payments in another namespace calls ledger: HTTP " + c.http(shadow, Lab.LEDGER.url("/hostname")) + "\n");
            out.append("--- who may create pods decides who may take an identity\n");
            Sh.Result webCan = c.kubectl("auth", "can-i", "create", "pods", "-n", "bank", "--as", "system:serviceaccount:bank:web");
            out.append("the web ServiceAccount may create pods in bank: " + webCan.output().strip() + "\n");
            c.must("-n", "bank", "create", "rolebinding", "dev-edit", "--clusterrole=edit", "--user=dev");
            Sh.Result devCan = c.kubectl("auth", "can-i", "create", "pods", "-n", "bank", "--as", "dev");
            out.append("a developer with the namespace's edit role may create pods in bank: " + devCan.output().strip() + "\n");

            out.append("--- certificate lifetime and rotation\n");
            out.append("Linkerd's default lifetime of a workload certificate: " + Linkerd.defaultIssuanceLifetime() + "\n");
            out.append("lifetime in this run: " + minutes(firstCert.lifetime()) + "\n");
            CertInfo anchor = CertInfo.fromPem(c.must("-n", "linkerd", "get", "configmap", "linkerd-identity-trust-roots", "-o", "jsonpath={.data.ca-bundle\\.crt}"));
            out.append("the trust anchor is valid for " + Duration.between(anchor.notBefore(), anchor.notAfter()).toDays() + " days\n");
            CertInfo later = firstCert;
            for (int i = 0; i < 60 && !later.notAfter().isAfter(firstCert.notAfter()); i++) {
                Thread.sleep(15_000);
                later = c.identity(Lab.PAYMENTS);
            }
            out.append("payments' proxy holds a newer certificate than before: " + yes(later.notAfter().isAfter(firstCert.notAfter())) + "\n");
            out.append("the name in it is the same: " + yes(later.subject().equals(firstCert.subject())) + "\n");
            out.append("the pod restarted to get it: " + yes(lab.restarts(Lab.PAYMENTS) > 0) + "\n");
            out.append("calls still answer after the renewal: payments calls ledger HTTP " + c.http(Lab.PAYMENTS, Lab.LEDGER.url("/hostname")) + "\n");
        }
        out.append("--- the bill\n");
        out.append("the mesh gives each pod a name taken from its ServiceAccount and encrypts between proxies: it does not decide who may call until a policy says so\n");
        out.append("a policy covers the ports that have a Server and the callers that go through a proxy\n");
        out.append("the mesh judges the caller, never the request: an allowed caller can send anything\n");
        out.append("the identity is only as good as who may create pods with a ServiceAccount\n");
        out.append("a certificate is trusted until it expires: the lifetime is how long a stolen one is useful\n");
        out.append("whoever has root on a node can read what the proxy hands to the app\n");
        return out.toString();
    }
}
