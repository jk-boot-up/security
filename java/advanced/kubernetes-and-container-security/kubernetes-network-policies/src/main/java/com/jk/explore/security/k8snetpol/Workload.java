package com.jk.explore.security.k8snetpol;

/** A pod of the online store: where it runs, its name, the label that policies select it by, and what it does. */
public record Workload(String namespace, String name, String app, String role) {

    /** The image every pod runs: a small web server with curl inside, pinned by version. */
    static final String IMAGE = "registry.k8s.io/e2e-test-images/agnhost:2.53";

    public String id() {
        return namespace + "/" + name;
    }

    /** The pod as a manifest: a small web server that answers on port 8080 and has curl for probing from inside. */
    public String manifest() {
        return """
                apiVersion: v1
                kind: Pod
                metadata:
                  name: %s
                  namespace: %s
                  labels: {app: %s}
                spec:
                  containers:
                    - name: main
                      image: %s
                      args: [netexec, --http-port=8080]
                      ports: [{containerPort: 8080}]
                """.formatted(name, namespace, app, IMAGE);
    }
}
