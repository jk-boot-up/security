package com.jk.explore.security.k8snetpol;

/** The online store's network policies as manifests, one small builder for each shape. */
public final class Policies {

    private Policies() {
    }

    /** Selects every pod of the namespace and allows no traffic in. */
    public static Policy denyAllIngress(String namespace) {
        return new Policy("deny-all-ingress", namespace, "all pods of " + namespace + ": nothing in", """
                apiVersion: networking.k8s.io/v1
                kind: NetworkPolicy
                metadata:
                  name: deny-all-ingress
                  namespace: %s
                spec:
                  podSelector: {}
                  policyTypes: [Ingress]
                """.formatted(namespace));
    }

    /** Denies ingress for the pods with one label only; the other pods of the namespace keep no policy at all. */
    public static Policy denyIngressFor(String namespace, String app) {
        return new Policy("deny-ingress-" + app, namespace, namespace + " app=" + app + " only: nothing in", """
                apiVersion: networking.k8s.io/v1
                kind: NetworkPolicy
                metadata:
                  name: deny-ingress-%s
                  namespace: %s
                spec:
                  podSelector:
                    matchLabels: {app: %s}
                  policyTypes: [Ingress]
                """.formatted(app, namespace, app));
    }

    /**
     * Lets the pods labelled {@code fromApp} in {@code fromNamespace} reach the pods labelled {@code app} on port 8080. Both
     * selectors sit in one element, so a caller needs both (an AND); a null {@code fromApp} admits the whole namespace.
     */
    public static Policy allowIngress(String namespace, String app, String fromNamespace, String fromApp) {
        String caller = fromApp == null ? fromNamespace : fromNamespace + "-" + fromApp;
        String podSelector = fromApp == null ? "" : "\n          podSelector:\n            matchLabels: {app: " + fromApp + "}";
        return new Policy("allow-" + app + "-from-" + caller, namespace,
                namespace + " app=" + app + " <- " + fromNamespace + (fromApp == null ? " any pod" : " app=" + fromApp), """
                apiVersion: networking.k8s.io/v1
                kind: NetworkPolicy
                metadata:
                  name: allow-%s-from-%s
                  namespace: %s
                spec:
                  podSelector:
                    matchLabels: {app: %s}
                  policyTypes: [Ingress]
                  ingress:
                    - from:
                        - namespaceSelector:
                            matchLabels: {kubernetes.io/metadata.name: %s}%s
                      ports: [{port: 8080}]
                """.formatted(app, caller, namespace, app, fromNamespace, podSelector));
    }

    /** Selects every pod of the namespace and allows no traffic out. */
    public static Policy denyAllEgress(String namespace) {
        return new Policy("deny-all-egress", namespace, "all pods of " + namespace + ": nothing out", """
                apiVersion: networking.k8s.io/v1
                kind: NetworkPolicy
                metadata:
                  name: deny-all-egress
                  namespace: %s
                spec:
                  podSelector: {}
                  policyTypes: [Egress]
                """.formatted(namespace));
    }

    /** Lets the pods labelled {@code app} send to the pods labelled {@code toApp} in {@code toNamespace}, on port 8080. */
    public static Policy allowEgress(String namespace, String app, String toNamespace, String toApp) {
        return new Policy("allow-" + app + "-to-" + toApp, namespace,
                namespace + " app=" + app + " -> " + toNamespace + " app=" + toApp, """
                apiVersion: networking.k8s.io/v1
                kind: NetworkPolicy
                metadata:
                  name: allow-%s-to-%s
                  namespace: %s
                spec:
                  podSelector:
                    matchLabels: {app: %s}
                  policyTypes: [Egress]
                  egress:
                    - to:
                        - namespaceSelector:
                            matchLabels: {kubernetes.io/metadata.name: %s}
                          podSelector:
                            matchLabels: {app: %s}
                      ports: [{port: 8080}]
                """.formatted(app, toApp, namespace, app, toNamespace, toApp));
    }

    /** Lets the pods of the namespace ask the cluster's DNS server (kube-dns in kube-system) for names, on port 53. */
    public static Policy allowDns(String namespace) {
        return new Policy("allow-dns", namespace, "all pods of " + namespace + " -> kube-dns, port 53", """
                apiVersion: networking.k8s.io/v1
                kind: NetworkPolicy
                metadata:
                  name: allow-dns
                  namespace: %s
                spec:
                  podSelector: {}
                  policyTypes: [Egress]
                  egress:
                    - to:
                        - namespaceSelector:
                            matchLabels: {kubernetes.io/metadata.name: kube-system}
                          podSelector:
                            matchLabels: {k8s-app: kube-dns}
                      ports:
                        - {port: 53, protocol: UDP}
                        - {port: 53, protocol: TCP}
                """.formatted(namespace));
    }
}
