package com.jk.explore.security.k8snetpol;

/** A pod of the bank: where it runs, its name, and the label that policies select it by. */
public record Workload(String namespace, String name, String app) {

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
                      image: registry.k8s.io/e2e-test-images/agnhost:2.53
                      args: [netexec, --http-port=8080]
                      ports: [{containerPort: 8080}]
                """.formatted(name, namespace, app);
    }
}
