package com.jk.explore.security.meshmtls;

/** A workload of the bank: the namespace it runs in, its name and the ServiceAccount its pod runs as. */
public record Service(String namespace, String name, String serviceAccount) {

    public Service(String namespace, String name) {
        this(namespace, name, name);
    }

    public String id() {
        return namespace + "/" + name;
    }

    /** The identity a Linkerd proxy takes from the pod's ServiceAccount: name, namespace, then the trust domain. */
    public String identity() {
        return serviceAccount + "." + namespace + ".serviceaccount.identity.linkerd.cluster.local";
    }

    /** The address other pods use for it. */
    public String url(String path) {
        return "http://" + name + "." + namespace + ":8080" + path;
    }

    /** A ServiceAccount (when the pod runs as its own), a Deployment of one small web server that has curl, and a Service. */
    public String manifest() {
        String account = serviceAccount.equals(name) ? """
                apiVersion: v1
                kind: ServiceAccount
                metadata: {name: %s, namespace: %s}
                ---
                """.formatted(serviceAccount, namespace) : "";
        return account + """
                apiVersion: apps/v1
                kind: Deployment
                metadata: {name: %1$s, namespace: %2$s}
                spec:
                  replicas: 1
                  selector: {matchLabels: {app: %1$s}}
                  template:
                    metadata: {labels: {app: %1$s}}
                    spec:
                      serviceAccountName: %3$s
                      containers:
                        - name: main
                          image: registry.k8s.io/e2e-test-images/agnhost:2.53
                          args: [netexec, --http-port=8080]
                          ports: [{containerPort: 8080}]
                ---
                apiVersion: v1
                kind: Service
                metadata: {name: %1$s, namespace: %2$s}
                spec:
                  selector: {app: %1$s}
                  ports: [{port: 8080}]
                """.formatted(name, namespace, serviceAccount);
    }
}
