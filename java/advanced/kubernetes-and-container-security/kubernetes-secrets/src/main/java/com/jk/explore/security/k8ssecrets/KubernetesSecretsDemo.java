package com.jk.explore.security.k8ssecrets;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class KubernetesSecretsDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static final String CARD_KEY = "demo-card-vault-key-001";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    /** Whether every Secret that etcd holds is encrypted. */
    static boolean allEncrypted(Cluster c) throws Exception {
        for (String key : c.etcdKeys("/registry/secrets/")) {
            if (!c.etcdValue(key).encrypted()) {
                return false;
            }
        }
        return true;
    }

    /** What the API server says about a token (a TokenReview). */
    static boolean accepted(Cluster c, String token, String audience) throws Exception {
        return c.create(Manifests.tokenReview(token, audience), "-o", "jsonpath={.status.authenticated}").output().trim().equals("true");
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        try (Cluster cluster = new Cluster()) {
            cluster.apply(Manifests.namespace("payments"));
            cluster.apply(Manifests.listSecretsRole("payments", "auditor"));
            cluster.apply(Manifests.serviceAccount("payments", "nobody"));
            cluster.must("-n", "payments", "create", "secret", "generic", "card-key", "--from-literal=key=" + CARD_KEY);

            out.append("--- base64 is an encoding, not encryption\n");
            String encoded = cluster.must("-n", "payments", "get", "secret", "card-key", "-o", "jsonpath={.data.key}");
            out.append("card-key as the API returns it: key=" + encoded + "\n");
            out.append("decoded by anyone, with no key: " + new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8) + "\n");
            Sh.Result listed = cluster.kubectl("--as=system:serviceaccount:payments:auditor", "-n", "payments", "get", "secrets", "-o", "json");
            boolean read = false;
            if (listed.ok()) {
                for (JsonNode item : JSON.readTree(listed.output()).get("items")) {
                    read |= item.path("data").path("key").asString("").equals(encoded);
                }
            }
            out.append("auditor, who may only list secrets, reads the value: " + yes(read) + "\n");
            Sh.Result denied = cluster.kubectl("--as=system:serviceaccount:payments:nobody", "-n", "payments", "get", "secrets");
            out.append("nobody, with no rule on secrets, reads the value: " + yes(denied.ok()) + "\n");

            out.append("--- in etcd, before encryption\n");
            Stored before = cluster.etcdValue("/registry/secrets/payments/card-key");
            out.append("the value etcd holds contains the key as plain text: " + yes(before.contains(CARD_KEY)) + "\n");
            out.append("it contains the base64 form: " + yes(before.contains(encoded)) + "\n");
            out.append("every secret in etcd is encrypted: " + yes(allEncrypted(cluster)) + "\n");

            out.append("--- encryption at rest\n");
            cluster.enableEncryption();
            Stored old = cluster.etcdValue("/registry/secrets/payments/card-key");
            out.append("API server restarted with an aescbc EncryptionConfiguration\n");
            out.append("card-key, written before: still plain text in etcd: " + yes(old.contains(CARD_KEY)) + "\n");
            cluster.must("-n", "payments", "create", "secret", "generic", "card-key-2", "--from-literal=key=" + CARD_KEY);
            Stored fresh = cluster.etcdValue("/registry/secrets/payments/card-key-2");
            out.append("card-key-2, written after: encrypted in etcd: " + yes(fresh.encrypted()) + ", plain text in etcd: " + yes(fresh.contains(CARD_KEY)) + "\n");
            out.append("kubectl still returns card-key-2 as before: " + yes(cluster.must("-n", "payments", "get", "secret", "card-key-2", "-o", "jsonpath={.data.key}").equals(encoded)) + "\n");
            out.append("every secret in etcd is encrypted: " + yes(allEncrypted(cluster)) + "\n");
            java.nio.file.Path all = java.nio.file.Files.createTempFile("secrets", ".json");
            java.nio.file.Files.writeString(all, cluster.must("get", "secrets", "-A", "-o", "json"));
            cluster.kubectl("replace", "-f", all.toString());
            Stored rewritten = cluster.etcdValue("/registry/secrets/payments/card-key");
            out.append("after rewriting every secret: card-key encrypted: " + yes(rewritten.encrypted()) + ", plain text: " + yes(rewritten.contains(CARD_KEY)) + "\n");
            out.append("every secret in etcd is encrypted: " + yes(allEncrypted(cluster)) + "\n");
            out.append("the card key anywhere in etcd: " + yes(cluster.etcdEverything().contains(CARD_KEY)) + "\n");
            out.append("the encryption key is a file on the control-plane node: " + yes(cluster.keyIsOnTheNode()) + "\n");
            Sh.Result again = cluster.kubectl("--as=system:serviceaccount:payments:auditor", "-n", "payments", "get", "secrets", "-o", "json");
            out.append("auditor still reads the value: " + yes(again.ok() && again.output().contains(encoded)) + "\n");

            out.append("--- an external secret and a short-lived token, OpenBao\n");
            cluster.apply(Manifests.serviceAccount("payments", "payments-app"));
            cluster.apply(Manifests.serviceAccount("payments", "other-app"));
            OpenBao bao = new OpenBao(cluster);
            bao.start();
            bao.configure("payments", "payments-app");
            cluster.apply(Manifests.app("payments", "payments-app", "payments-app"));
            cluster.apply(Manifests.app("payments", "other-app", "other-app"));
            cluster.must("-n", "payments", "wait", "--for=condition=Ready", "pod/payments-app", "pod/other-app", "--timeout=240s");
            out.append("OpenBao in the cluster, with the database password stored in it: yes\n");
            String b64 = Base64.getEncoder().encodeToString(OpenBao.SECRET_VALUE.getBytes(StandardCharsets.UTF_8));
            out.append("Kubernetes Secrets in the cluster that hold the database password: " + (cluster.must("get", "secrets", "-A", "-o", "json").contains(b64) ? "some" : "none") + "\n");
            Http login = bao.login("payments", "payments-app", "payments");
            out.append("payments-app logs in with its ServiceAccount token: HTTP " + login.status() + "\n");
            String client = login.body().path("auth").path("client_token").asString("");
            out.append("lifetime of the OpenBao token it gets: " + login.body().path("auth").path("lease_duration").asInt() + " seconds\n");
            Http value = bao.read("payments", "payments-app", client);
            out.append("it reads the database password: HTTP " + value.status() + ", the right value: " + yes(value.body().path("data").path("data").path("password").asString("").equals(OpenBao.SECRET_VALUE)) + "\n");
            out.append("other-app (another ServiceAccount) logs in: HTTP " + bao.login("payments", "other-app", "payments").status() + "\n");
            Thread.sleep(12_000);
            out.append("payments-app reads again 12 seconds later, same token: HTTP " + bao.read("payments", "payments-app", client).status() + "\n");
            out.append("the database password anywhere in etcd: " + yes(cluster.etcdEverything().contains(OpenBao.SECRET_VALUE)) + "\n");

            out.append("--- a bound ServiceAccount token\n");
            cluster.apply("""
                    apiVersion: v1
                    kind: Pod
                    metadata: {name: report-job, namespace: payments}
                    spec:
                      serviceAccountName: payments-app
                      terminationGracePeriodSeconds: 0
                      containers: [{name: job, image: %s, command: [sleep, "3600"]}]
                    """.formatted(Manifests.APP_IMAGE));
            cluster.must("-n", "payments", "wait", "--for=condition=Ready", "pod/report-job", "--timeout=240s");
            String bound = cluster.must("-n", "payments", "create", "token", "payments-app", "--audience=" + Manifests.AUDIENCE, "--duration=10m", "--bound-object-kind=Pod", "--bound-object-name=report-job").trim();
            Jwt b = Jwt.parse(bound);
            out.append("requested for 10 minutes: lifetime " + b.lifetimeSeconds() + " seconds, audience openbao: " + yes(b.hasAudience(Manifests.AUDIENCE)) + ", tied to the pod report-job: " + yes(b.boundPod().equals("report-job")) + "\n");
            cluster.apply(Manifests.legacyTokenSecret("payments", "payments-app"));
            String legacy = "";
            for (int i = 0; i < 30 && legacy.isEmpty(); i++) {
                legacy = new String(Base64.getDecoder().decode(cluster.must("-n", "payments", "get", "secret", "payments-app-legacy-token", "-o", "jsonpath={.data.token}").trim()), StandardCharsets.UTF_8);
                if (legacy.isEmpty()) {
                    Thread.sleep(1000);
                }
            }
            Jwt l = Jwt.parse(legacy);
            out.append("the old token Secret: has an expiry: " + yes(l.hasExpiry()) + ", tied to a pod: " + yes(!l.boundPod().isEmpty()) + "\n");
            out.append("both accepted by the API server while report-job runs: " + yes(accepted(cluster, bound, Manifests.AUDIENCE) && accepted(cluster, legacy, "")) + "\n");
            cluster.must("-n", "payments", "delete", "pod", "report-job", "--grace-period=0", "--force", "--wait=true");
            // the API server remembers the answer about the pod for a few seconds, so wait until it stops accepting the token
            boolean boundStill = true;
            for (int i = 0; i < 45 && boundStill; i++) {
                boundStill = accepted(cluster, bound, Manifests.AUDIENCE);
                if (boundStill) {
                    Thread.sleep(2000);
                }
            }
            out.append("after report-job is deleted (the API server caches the pod for a few seconds): the bound token is accepted: " + yes(boundStill) + ", the old one: " + yes(accepted(cluster, legacy, "")) + "\n");
        }
        out.append("--- the bill\n");
        out.append("base64 hides nothing; etcd encryption protects a disk or a backup of etcd, not a person with the right to read secrets\n");
        out.append("the key of the encryption sits on the control plane here; a KMS provider keeps it outside\n");
        out.append("a development-mode OpenBao is in memory with a known root token; a real one needs storage, unsealing and an audit log\n");
        return out.toString();
    }
}
