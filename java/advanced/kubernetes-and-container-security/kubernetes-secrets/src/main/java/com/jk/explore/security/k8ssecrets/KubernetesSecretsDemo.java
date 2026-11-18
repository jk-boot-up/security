package com.jk.explore.security.k8ssecrets;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class KubernetesSecretsDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static final String CARD_KEY = "demo-card-vault-key-001";
    static final String CARD_PATH = "/registry/secrets/payments/card-key";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    /** "k of n": how many of the Secrets that etcd holds are encrypted. */
    static String encryptedCount(Cluster c) throws Exception {
        List<String> keys = c.etcdKeys("/registry/secrets/");
        int encrypted = 0;
        for (String key : keys) {
            encrypted += c.etcdValue(key).encrypted() ? 1 : 0;
        }
        return encrypted + " of " + keys.size();
    }

    /** What the API server says about a token (a TokenReview). */
    static boolean accepted(Cluster c, String token, String audience) throws Exception {
        return c.create(Manifests.tokenReview(token, audience), "-o", "jsonpath={.status.authenticated}").output().trim().equals("true");
    }

    static boolean readsTheValue(Sh.Result listed, String encoded) {
        boolean read = false;
        if (listed.ok()) {
            for (JsonNode item : JSON.readTree(listed.output()).get("items")) {
                read |= item.path("data").path("key").asString("").equals(encoded);
            }
        }
        return read;
    }

    static String hex(List<Integer> bytes) {
        return String.join(" ", bytes.stream().map(b -> String.format("%02x", b)).toList());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        try (Cluster cluster = new Cluster()) {
            cluster.apply(Manifests.namespace("payments"));
            cluster.apply(Manifests.listSecretsRole("payments", "auditor"));
            cluster.apply(Manifests.serviceAccount("payments", "nobody"));
            cluster.must("-n", "payments", "create", "secret", "generic", "card-key", "--from-literal=key=" + CARD_KEY);

            out.append("ONE. Base64 is an encoding, not encryption.\n");
            out.append("  THE SECRET (kubectl create secret generic):\n");
            out.append("    name   payments/card-key\n");
            out.append("    key    " + CARD_KEY + "\n");
            String encoded = cluster.must("-n", "payments", "get", "secret", "card-key", "-o", "jsonpath={.data.key}");
            out.append("  WHAT THE API RETURNS (data.key):\n");
            out.append("    " + encoded + "\n");
            Base64Steps g = Base64Steps.firstGroup(encoded);
            out.append("  HOW BASE64 IS UNDONE (first 4 characters, no key):\n");
            out.append("    characters  " + String.join(", ", g.chars().split("")) + "\n");
            out.append("    place in    " + String.join(" ", g.sixBitValues().stream().map(String::valueOf).toList()) + " (A=0 ... Z=25, a=26 ... z=51)\n");
            out.append("    6 bits      " + g.sixBits() + "\n");
            out.append("    8 bits      " + g.eightBits() + "\n");
            out.append("    bytes       " + String.join(" ", g.bytes().stream().map(String::valueOf).toList()) + " = \"" + g.text() + "\"\n");
            out.append("    all of it   " + new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8) + "\n");
            out.append("  WHO CAN READ IT:\n");
            Sh.Result listed = cluster.kubectl("--as=system:serviceaccount:payments:auditor", "-n", "payments", "get", "secrets", "-o", "json");
            out.append("    auditor (may only list secrets)  reads the value: " + yes(readsTheValue(listed, encoded)) + "\n");
            Sh.Result denied = cluster.kubectl("--as=system:serviceaccount:payments:nobody", "-n", "payments", "get", "secrets");
            out.append("    nobody (no rule on secrets)      reads the value: " + yes(denied.ok()) + "\n");

            out.append("TWO. Raw etcd, before encryption.\n");
            out.append("  THE READ (etcdctl, inside the etcd pod):\n");
            out.append("    get " + CARD_PATH + "\n");
            Stored before = cluster.etcdValue(CARD_PATH);
            out.append("  WHAT ETCD HOLDS:\n");
            out.append("    starts with      " + before.start(20) + "  (. = not a letter)\n");
            out.append("    the card key     as plain text: " + yes(before.contains(CARD_KEY)) + "\n");
            out.append("    its base64 form  " + yes(before.contains(encoded)) + "\n");
            List<Integer> front = before.bytesBefore(CARD_KEY, 7);
            out.append("  THE 7 BYTES IN FRONT OF THE CARD KEY (hex):\n");
            out.append("    " + hex(front.subList(0, 2)) + "     field " + (front.get(0) >> 3) + ", length " + front.get(1) + "\n");
            out.append("    " + hex(front.subList(2, 5)) + "  \"key\", the name in data\n");
            out.append("    " + hex(front.subList(5, 7)) + "     field " + (front.get(5) >> 3) + ", length 0x" + hex(front.subList(6, 7)) + " = " + front.get(6) + "\n");
            out.append("    then      " + CARD_KEY + " (" + CARD_KEY.length() + " characters)\n");
            out.append("  EVERY SECRET IN ETCD:\n");
            out.append("    encrypted   " + encryptedCount(cluster) + "\n");

            out.append("THREE. Encryption at rest.\n");
            cluster.enableEncryption();
            out.append("  THE ENCRYPTION CONFIGURATION (the API server restarted):\n");
            out.append("    resources   secrets\n");
            out.append("    provider 1  aescbc, key1: " + Encryption.KEY_BYTES + " random bytes, not printed\n");
            out.append("    provider 2  identity: reads what was stored plain\n");
            Stored old = cluster.etcdValue(CARD_PATH);
            cluster.must("-n", "payments", "create", "secret", "generic", "card-key-2", "--from-literal=key=" + CARD_KEY);
            Stored fresh = cluster.etcdValue("/registry/secrets/payments/card-key-2");
            out.append("  BEFORE THE REWRITE:\n");
            out.append("    card-key (written before)  plain text: " + yes(old.contains(CARD_KEY)) + "\n");
            out.append("    card-key-2 (written after)  encrypted: " + yes(fresh.encrypted()) + ", plain text: " + yes(fresh.contains(CARD_KEY)) + "\n");
            out.append("    kubectl returns card-key-2 as before: " + yes(cluster.must("-n", "payments", "get", "secret", "card-key-2", "-o", "jsonpath={.data.key}").equals(encoded)) + "\n");
            out.append("    secrets encrypted  " + encryptedCount(cluster) + "\n");
            out.append("  WHAT ETCD HOLDS FOR card-key-2:\n");
            out.append("    starts with  " + Encryption.MARKER + ":\n");
            List<String> parts = fresh.markerParts();
            List<String> meaning = List.of("encrypted by the API server", "the provider: AES in CBC mode", "the provider's format version", "the key's name in the configuration");
            for (int i = 0; i < parts.size(); i++) {
                out.append("    " + parts.get(i) + " ".repeat(10 - parts.get(i).length()) + meaning.get(i) + "\n");
            }
            out.append("    then      bytes that look random; \"Secret\" in them: " + yes(fresh.raw().substring(Encryption.MARKER.length()).contains("Secret")) + "\n");
            Path all = Files.createTempFile("secrets", ".json");
            Files.writeString(all, cluster.must("get", "secrets", "-A", "-o", "json"));
            cluster.kubectl("replace", "-f", all.toString());
            Stored rewritten = cluster.etcdValue(CARD_PATH);
            out.append("  AFTER THE REWRITE (kubectl replace of every secret):\n");
            out.append("    card-key  encrypted: " + yes(rewritten.encrypted()) + ", plain text: " + yes(rewritten.contains(CARD_KEY)) + "\n");
            out.append("    secrets encrypted  " + encryptedCount(cluster) + "\n");
            out.append("    the card key anywhere in etcd: " + yes(cluster.etcdEverything().contains(CARD_KEY)) + "\n");
            out.append("  WHAT IT DOES NOT CHANGE:\n");
            out.append("    the key is a file on the control-plane node: " + yes(cluster.keyIsOnTheNode()) + "\n");
            Sh.Result again = cluster.kubectl("--as=system:serviceaccount:payments:auditor", "-n", "payments", "get", "secrets", "-o", "json");
            out.append("    the auditor still reads the value: " + yes(readsTheValue(again, encoded)) + "\n");

            out.append("FOUR. OpenBao and a 10 second token.\n");
            cluster.apply(Manifests.serviceAccount("payments", "payments-app"));
            cluster.apply(Manifests.serviceAccount("payments", "other-app"));
            OpenBao bao = new OpenBao(cluster);
            bao.start();
            bao.configure("payments", "payments-app");
            cluster.apply(Manifests.app("payments", "payments-app", "payments-app"));
            cluster.apply(Manifests.app("payments", "other-app", "other-app"));
            cluster.must("-n", "payments", "wait", "--for=condition=Ready", "pod/payments-app", "pod/other-app", "--timeout=240s");
            out.append("  THE ROLE IN OPENBAO (auth/kubernetes/role/payments):\n");
            out.append("    who       ServiceAccount payments/payments-app\n");
            out.append("    audience  " + Manifests.AUDIENCE + "\n");
            out.append("    policy    " + OpenBao.POLICY + ": read " + OpenBao.SECRET_PATH + "\n");
            out.append("    token     " + OpenBao.TOKEN_TTL_SECONDS + " seconds\n");
            String b64 = Base64.getEncoder().encodeToString(OpenBao.SECRET_VALUE.getBytes(StandardCharsets.UTF_8));
            out.append("  WHERE THE DATABASE PASSWORD IS:\n");
            out.append("    in OpenBao: yes, in a Kubernetes Secret: " + (cluster.must("get", "secrets", "-A", "-o", "json").contains(b64) ? "yes" : "no")
                    + ", in etcd: " + yes(cluster.etcdEverything().contains(OpenBao.SECRET_VALUE)) + "\n");
            Jwt podToken = bao.podToken("payments", "payments-app");
            out.append("  THE LOGIN (payments-app sends its pod's token):\n");
            out.append("    sub       " + podToken.subject() + "\n");
            out.append("    aud       " + podToken.audiences() + "\n");
            Http login = bao.login("payments", "payments-app", "payments");
            String client = login.body().path("auth").path("client_token").asString("");
            JsonNode auth = login.body().path("auth");
            out.append("  THE RESPONSE:\n");
            out.append("    HTTP " + login.status() + ", a token for " + auth.path("lease_duration").asInt() + " seconds\n");
            out.append("    policies  " + String.join(", ", auth.path("token_policies").valueStream().map(JsonNode::asString).toList()) + "\n");
            Http value = bao.read("payments", "payments-app", client);
            out.append("  READING THE PASSWORD WITH THAT TOKEN:\n");
            out.append("    at once     HTTP " + value.status() + ", the right value: " + yes(value.body().path("data").path("data").path("password").asString("").equals(OpenBao.SECRET_VALUE)) + "\n");
            Thread.sleep(12_000);
            out.append("    12 s later  12 > " + OpenBao.TOKEN_TTL_SECONDS + ", expired: HTTP " + bao.read("payments", "payments-app", client).status() + "\n");
            Jwt otherToken = bao.podToken("payments", "other-app");
            out.append("  ANOTHER POD LOGS IN (other-app):\n");
            out.append("    sub       " + otherToken.subject() + "\n");
            out.append("    HTTP " + bao.login("payments", "other-app", "payments").status() + ": not the role's ServiceAccount\n");

            out.append("FIVE. A bound ServiceAccount token.\n");
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
            out.append("  THE BOUND TOKEN (kubectl create token, 10 minutes):\n");
            out.append("    claims    " + String.join(" ", b.claimNames()) + "\n");
            out.append("    lifetime  exp - iat = " + b.lifetimeSeconds() + " seconds\n");
            out.append("    audience  " + b.audiences() + "\n");
            out.append("    pod       " + b.boundPod() + "\n");
            cluster.apply(Manifests.legacyTokenSecret("payments", "payments-app"));
            String legacy = "";
            for (int i = 0; i < 30 && legacy.isEmpty(); i++) {
                legacy = new String(Base64.getDecoder().decode(cluster.must("-n", "payments", "get", "secret", "payments-app-legacy-token", "-o", "jsonpath={.data.token}").trim()), StandardCharsets.UTF_8);
                if (legacy.isEmpty()) {
                    Thread.sleep(1000);
                }
            }
            Jwt l = Jwt.parse(legacy);
            out.append("  THE OLD TOKEN (a Secret of type service-account-token):\n");
            out.append("    claims    " + l.claimNames().size() + ", among them exp: " + yes(l.hasExpiry()) + ", aud: " + yes(!l.audiences().isEmpty()) + "\n");
            out.append("    lifetime  " + (l.hasExpiry() ? l.lifetimeSeconds() + " seconds" : "no exp: it never expires") + "\n");
            out.append("    pod       " + (l.boundPod().isEmpty() ? "none" : l.boundPod()) + "\n");
            out.append("  THE API SERVER'S ANSWER (a TokenReview of each):\n");
            out.append("    while report-job runs  bound: " + yes(accepted(cluster, bound, Manifests.AUDIENCE)) + ", old: " + yes(accepted(cluster, legacy, "")) + "\n");
            cluster.must("-n", "payments", "delete", "pod", "report-job", "--grace-period=0", "--force", "--wait=true");
            // the API server remembers the answer about the pod for a few seconds, so wait until it stops accepting the token
            boolean boundStill = true;
            for (int i = 0; i < 45 && boundStill; i++) {
                boundStill = accepted(cluster, bound, Manifests.AUDIENCE);
                if (boundStill) {
                    Thread.sleep(2000);
                }
            }
            out.append("    after it is deleted    bound: " + yes(boundStill) + ", old: " + yes(accepted(cluster, legacy, "")) + "\n");
        }
        return out.toString();
    }
}
