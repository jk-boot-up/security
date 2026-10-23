package com.jk.explore.security.k8snetpol;

/** The bank's network policies as manifests, one small builder for each shape. */
public final class Policies {

    private Policies() {
    }

    /** Selects every pod of the namespace and allows no traffic in. */
    public static String denyAllIngress(String namespace) {
        return """
                apiVersion: networking.k8s.io/v1
                kind: NetworkPolicy
                metadata: {name: deny-all-ingress, namespace: %s}
                spec:
                  podSelector: {}
                  policyTypes: [Ingress]
                """.formatted(namespace);
    }

    /** Denies ingress for the pods with one label only; a typo in the label selects nothing. */
    public static String denyIngressFor(String namespace, String app) {
        return """
                apiVersion: networking.k8s.io/v1
                kind: NetworkPolicy
                metadata: {name: deny-ingress-%s, namespace: %s}
                spec:
                  podSelector: {matchLabels: {app: %s}}
                  policyTypes: [Ingress]
                """.formatted(app, namespace, app);
    }

    /** Lets the pods labelled {@code fromApp} in {@code fromNamespace} reach the pods labelled {@code app} (both selectors in one element: an AND). */
    public static String allowIngress(String namespace, String app, String fromNamespace, String fromApp) {
        String from = fromApp == null
                ? "- namespaceSelector: {matchLabels: {kubernetes.io/metadata.name: %s}}".formatted(fromNamespace)
                : "- namespaceSelector: {matchLabels: {kubernetes.io/metadata.name: %s}}\n        podSelector: {matchLabels: {app: %s}}".formatted(fromNamespace, fromApp);
        return """
                apiVersion: networking.k8s.io/v1
                kind: NetworkPolicy
                metadata: {name: allow-%s-from-%s, namespace: %s}
                spec:
                  podSelector: {matchLabels: {app: %s}}
                  policyTypes: [Ingress]
                  ingress:
                    - from:
                      %s
                      ports: [{port: 8080}]
                """.formatted(app, fromApp == null ? fromNamespace : fromNamespace + "-" + fromApp, namespace, app, from);
    }

    /** Selects every pod of the namespace and allows no traffic out. */
    public static String denyAllEgress(String namespace) {
        return """
                apiVersion: networking.k8s.io/v1
                kind: NetworkPolicy
                metadata: {name: deny-all-egress, namespace: %s}
                spec:
                  podSelector: {}
                  policyTypes: [Egress]
                """.formatted(namespace);
    }

    /** Lets the pods labelled {@code app} send to the pods labelled {@code toApp} in {@code toNamespace}. */
    public static String allowEgress(String namespace, String app, String toNamespace, String toApp) {
        return """
                apiVersion: networking.k8s.io/v1
                kind: NetworkPolicy
                metadata: {name: allow-%s-to-%s, namespace: %s}
                spec:
                  podSelector: {matchLabels: {app: %s}}
                  policyTypes: [Egress]
                  egress:
                    - to:
                        - namespaceSelector: {matchLabels: {kubernetes.io/metadata.name: %s}}
                          podSelector: {matchLabels: {app: %s}}
                      ports: [{port: 8080}]
                """.formatted(app, toApp, namespace, app, toNamespace, toApp);
    }

    /** Lets the pods of the namespace ask the cluster's DNS for names. */
    public static String allowDns(String namespace) {
        return """
                apiVersion: networking.k8s.io/v1
                kind: NetworkPolicy
                metadata: {name: allow-dns, namespace: %s}
                spec:
                  podSelector: {}
                  policyTypes: [Egress]
                  egress:
                    - to:
                        - namespaceSelector: {matchLabels: {kubernetes.io/metadata.name: kube-system}}
                          podSelector: {matchLabels: {k8s-app: kube-dns}}
                      ports:
                        - {port: 53, protocol: UDP}
                        - {port: 53, protocol: TCP}
                """.formatted(namespace);
    }
}
