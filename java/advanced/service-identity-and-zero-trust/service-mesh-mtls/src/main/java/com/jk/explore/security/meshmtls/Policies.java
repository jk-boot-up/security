package com.jk.explore.security.meshmtls;

import java.util.List;
import java.util.stream.Collectors;

/** Linkerd's policy objects: a Server names a port of a workload, an AuthorizationPolicy says which mesh identities may use it. */
public final class Policies {

    private Policies() {
    }

    /**
     * The policy for one workload's port 8080. A Server alone refuses every caller; each identity listed is let in, and only over mutual TLS.
     */
    public static String forService(Service target, List<Service> callers) {
        String server = """
                apiVersion: policy.linkerd.io/v1beta3
                kind: Server
                metadata: {name: %1$s-http, namespace: %2$s}
                spec:
                  podSelector: {matchLabels: {app: %1$s}}
                  port: 8080
                  proxyProtocol: HTTP/1
                """.formatted(target.name(), target.namespace());
        if (callers.isEmpty()) {
            return server;
        }
        String identities = callers.stream().map(c -> "\"" + c.identity() + "\"").collect(Collectors.joining(", "));
        return server + """
                ---
                apiVersion: policy.linkerd.io/v1alpha1
                kind: MeshTLSAuthentication
                metadata: {name: %1$s-callers, namespace: %2$s}
                spec:
                  identities: [%3$s]
                ---
                apiVersion: policy.linkerd.io/v1alpha1
                kind: AuthorizationPolicy
                metadata: {name: %1$s-allow-callers, namespace: %2$s}
                spec:
                  targetRef: {group: policy.linkerd.io, kind: Server, name: %1$s-http}
                  requiredAuthenticationRefs:
                    - {group: policy.linkerd.io, kind: MeshTLSAuthentication, name: %1$s-callers}
                """.formatted(target.name(), target.namespace(), identities);
    }
}
