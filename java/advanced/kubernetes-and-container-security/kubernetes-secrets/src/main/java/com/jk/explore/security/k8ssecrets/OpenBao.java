package com.jk.explore.security.k8ssecrets;

import java.io.IOException;

/** Sets up OpenBao in the cluster (policy, Kubernetes login) and talks to it from a pod, the way an application would. */
final class OpenBao {

    static final String SECRET_VALUE = "demo-db-password-7";
    private static final String URL = "http://openbao.vault.svc:8200";

    private final Cluster cluster;

    OpenBao(Cluster cluster) {
        this.cluster = cluster;
    }

    void start() throws IOException, InterruptedException {
        cluster.apply(Manifests.namespace("vault"));
        cluster.apply(Manifests.openbao());
        cluster.must("-n", "vault", "wait", "--for=condition=Ready", "pod/openbao", "--timeout=240s");
    }

    /** Runs bao inside the OpenBao pod with the root token: this is the operator's side. */
    private void admin(String command) throws IOException, InterruptedException {
        Sh.Result r = cluster.exec("vault", "openbao", "sh", "-c", command);
        if (!r.ok()) {
            throw new IllegalStateException("bao " + command + " failed: " + r.output());
        }
    }

    /** Stores the password, lets pods of one ServiceAccount log in and read only it, and gives the token 10 seconds. */
    void configure(String namespace, String serviceAccount) throws IOException, InterruptedException {
        admin("bao kv put -mount=secret payments/db password=" + SECRET_VALUE);
        admin("bao auth enable kubernetes");
        admin("bao write auth/kubernetes/config kubernetes_host=https://kubernetes.default.svc:443");
        admin("echo 'path \"secret/data/payments/db\" { capabilities = [\"read\"] }' | bao policy write payments-read -");
        admin("bao write auth/kubernetes/role/payments bound_service_account_names=" + serviceAccount + " bound_service_account_namespaces=" + namespace
                + " audience=" + Manifests.AUDIENCE + " token_policies=payments-read token_ttl=10s token_max_ttl=10s");
    }

    private Http call(String namespace, String pod, String method, String path, String token, String body) throws IOException, InterruptedException {
        String script = "curl -s -w '\\n%{http_code}' -X " + method + " -H 'X-Vault-Token: " + token + "' " + (body.isEmpty() ? "" : "-d \"" + body + "\" ") + URL + path;
        return Http.parse(cluster.exec(namespace, pod, "sh", "-c", script).output());
    }

    /** The pod logs in with the token the kubelet put in its volume. */
    Http login(String namespace, String pod, String role) throws IOException, InterruptedException {
        String body = "{\\\"role\\\":\\\"" + role + "\\\",\\\"jwt\\\":\\\"$(cat " + Manifests.TOKEN_PATH + ")\\\"}";
        return call(namespace, pod, "POST", "/v1/auth/kubernetes/login", "", body);
    }

    Http read(String namespace, String pod, String clientToken) throws IOException, InterruptedException {
        return call(namespace, pod, "GET", "/v1/secret/data/payments/db", clientToken, "");
    }
}
