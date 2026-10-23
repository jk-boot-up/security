package com.jk.explore.security.runtimesec;

/** The manifests of the bank's pods. */
public final class Pods {

    public static final String NAMESPACE = "bank";
    public static final String API = "payments-api";
    public static final String API_RO = "payments-api-ro";
    public static final String LEDGER = "ledger-db";
    public static final String IMAGE = "busybox:1.37";

    private Pods() {
    }

    /** The payments service: a web server started directly (no shell), a log folder it may write to, and optionally a read-only root file system. */
    public static String api(String name, boolean readOnlyRoot) {
        return """
                apiVersion: v1
                kind: Pod
                metadata:
                  name: %s
                  namespace: %s
                  labels: {app: %s}
                spec:
                  containers:
                  - name: app
                    image: %s
                    command: ["httpd", "-f", "-p", "8080", "-h", "/tmp"]
                    securityContext: {readOnlyRootFilesystem: %s}
                    volumeMounts:
                    - {name: logs, mountPath: /var/log/payments}
                  volumes:
                  - {name: logs, emptyDir: {}}
                """.formatted(name, NAMESPACE, name, IMAGE, readOnlyRoot);
    }

    /** The ledger database's stand-in: a server on port 5432 and the service in front of it. */
    public static String ledger() {
        return """
                apiVersion: v1
                kind: Pod
                metadata:
                  name: %s
                  namespace: %s
                  labels: {app: %s}
                spec:
                  containers:
                  - name: app
                    image: %s
                    command: ["httpd", "-f", "-p", "5432", "-h", "/tmp"]
                ---
                apiVersion: v1
                kind: Service
                metadata:
                  name: %s
                  namespace: %s
                spec:
                  selector: {app: %s}
                  ports:
                  - {port: 5432, targetPort: 5432}
                """.formatted(LEDGER, NAMESPACE, LEDGER, IMAGE, LEDGER, NAMESPACE, LEDGER);
    }
}
