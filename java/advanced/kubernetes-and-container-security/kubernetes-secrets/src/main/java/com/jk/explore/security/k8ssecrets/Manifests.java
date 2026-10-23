package com.jk.explore.security.k8ssecrets;

/** The Kubernetes objects of the demo, as text. */
final class Manifests {

    static final String OPENBAO_IMAGE = "openbao/openbao:2.7.1";
    static final String APP_IMAGE = "registry.k8s.io/e2e-test-images/agnhost:2.53";
    static final String TOKEN_PATH = "/var/run/secrets/openbao/token";
    static final String AUDIENCE = "openbao";

    private Manifests() {
    }

    static String namespace(String name) {
        return """
                apiVersion: v1
                kind: Namespace
                metadata:
                  name: %s
                """.formatted(name);
    }

    /** A Role that lets one ServiceAccount list Secrets in a namespace, and nothing else. */
    static String listSecretsRole(String namespace, String serviceAccount) {
        return """
                apiVersion: v1
                kind: ServiceAccount
                metadata: {name: %1$s, namespace: %2$s}
                ---
                apiVersion: rbac.authorization.k8s.io/v1
                kind: Role
                metadata: {name: %1$s-lists-secrets, namespace: %2$s}
                rules:
                  - apiGroups: [""]
                    resources: [secrets]
                    verbs: [list]
                ---
                apiVersion: rbac.authorization.k8s.io/v1
                kind: RoleBinding
                metadata: {name: %1$s-lists-secrets, namespace: %2$s}
                roleRef: {apiGroup: rbac.authorization.k8s.io, kind: Role, name: %1$s-lists-secrets}
                subjects:
                  - {kind: ServiceAccount, name: %1$s, namespace: %2$s}
                """.formatted(serviceAccount, namespace);
    }

    static String serviceAccount(String namespace, String name) {
        return """
                apiVersion: v1
                kind: ServiceAccount
                metadata: {name: %s, namespace: %s}
                """.formatted(name, namespace);
    }

    /**
     * OpenBao in development mode: one pod, in memory, unsealed, a fixed root token. It is a stand-in for a real server
     * (which would keep its data in raft storage and unseal itself with a cloud key), but it speaks the same API.
     * Its ServiceAccount may ask the API server who a token belongs to (the auth-delegator role).
     */
    static String openbao() {
        return """
                apiVersion: v1
                kind: ServiceAccount
                metadata: {name: openbao, namespace: vault}
                ---
                apiVersion: rbac.authorization.k8s.io/v1
                kind: ClusterRoleBinding
                metadata: {name: openbao-token-review}
                roleRef: {apiGroup: rbac.authorization.k8s.io, kind: ClusterRole, name: system:auth-delegator}
                subjects:
                  - {kind: ServiceAccount, name: openbao, namespace: vault}
                ---
                apiVersion: v1
                kind: Pod
                metadata: {name: openbao, namespace: vault, labels: {app: openbao}}
                spec:
                  serviceAccountName: openbao
                  containers:
                    - name: openbao
                      image: %s
                      args: ["server", "-dev", "-dev-root-token-id=root", "-dev-listen-address=0.0.0.0:8200"]
                      env:
                        - {name: BAO_ADDR, value: "http://127.0.0.1:8200"}
                        - {name: BAO_TOKEN, value: root}
                      ports: [{containerPort: 8200}]
                      readinessProbe:
                        httpGet: {path: /v1/sys/health, port: 8200}
                ---
                apiVersion: v1
                kind: Service
                metadata: {name: openbao, namespace: vault}
                spec:
                  selector: {app: openbao}
                  ports: [{port: 8200}]
                """.formatted(OPENBAO_IMAGE);
    }

    /** A pod that runs as a ServiceAccount and gets a token for OpenBao: short-lived, for that audience only, tied to the pod. */
    static String app(String namespace, String name, String serviceAccount) {
        return """
                apiVersion: v1
                kind: Pod
                metadata: {name: %s, namespace: %s}
                spec:
                  serviceAccountName: %s
                  terminationGracePeriodSeconds: 0
                  containers:
                    - name: app
                      image: %s
                      command: ["sleep", "3600"]
                      volumeMounts:
                        - {name: token, mountPath: /var/run/secrets/openbao, readOnly: true}
                  volumes:
                    - name: token
                      projected:
                        sources:
                          - serviceAccountToken: {audience: %s, expirationSeconds: 600, path: token}
                """.formatted(name, namespace, serviceAccount, APP_IMAGE, AUDIENCE);
    }

    /** The old kind of token: a Secret that Kubernetes fills with a token that has no expiry and is not tied to a pod. */
    static String legacyTokenSecret(String namespace, String serviceAccount) {
        return """
                apiVersion: v1
                kind: Secret
                metadata:
                  name: %1$s-legacy-token
                  namespace: %2$s
                  annotations: {kubernetes.io/service-account.name: %1$s}
                type: kubernetes.io/service-account-token
                """.formatted(serviceAccount, namespace);
    }

    static String tokenReview(String token, String audience) {
        String audiences = audience.isEmpty() ? "" : "  audiences: [" + audience + "]\n";
        return """
                apiVersion: authentication.k8s.io/v1
                kind: TokenReview
                spec:
                  token: %s
                %s""".formatted(token, audiences);
    }
}
