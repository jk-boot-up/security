package com.jk.explore.security.iacscan;

/** The online store's Kubernetes manifests, as first written and as fixed. */
public final class Manifests {

    private Manifests() {
    }

    static final String FIRST = """
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: web
            spec:
              replicas: 2
              selector:
                matchLabels: {app: web}
              template:
                metadata:
                  labels: {app: web}
                spec:
                  containers:
                    - name: web
                      image: shop/web:latest
                      ports:
                        - containerPort: 8080
                      securityContext:
                        privileged: true
            ---
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: orders
            spec:
              replicas: 2
              selector:
                matchLabels: {app: orders}
              template:
                metadata:
                  labels: {app: orders}
                spec:
                  containers:
                    - name: orders
                      image: shop/orders
                      env:
                        - name: DB_PASSWORD
                          value: hunter2
                        - name: REGION
                          value: eu-west-1
            ---
            apiVersion: apps/v1
            kind: DaemonSet
            metadata:
              name: metrics-agent
            spec:
              selector:
                matchLabels: {app: metrics-agent}
              template:
                metadata:
                  labels: {app: metrics-agent}
                spec:
                  hostNetwork: true
                  containers:
                    - name: agent
                      image: shop/metrics-agent:2.4.1
                      resources:
                        limits: {cpu: 100m, memory: 64Mi}
                      securityContext:
                        runAsNonRoot: true
                        readOnlyRootFilesystem: true
                        allowPrivilegeEscalation: false
            """;

    static String hardenedContainer(String name, String image, String extraEnv) {
        return """
                      - name: %s
                        image: %s
                        resources:
                          limits: {cpu: 500m, memory: 256Mi}
                          requests: {cpu: 100m, memory: 128Mi}
                        securityContext:
                          runAsNonRoot: true
                          readOnlyRootFilesystem: true
                          allowPrivilegeEscalation: false
                          capabilities: {drop: [ALL]}
                %s""".formatted(name, image, extraEnv);
    }

    static final String FIXED = """
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: web
            spec:
              replicas: 2
              selector:
                matchLabels: {app: web}
              template:
                metadata:
                  labels: {app: web}
                spec:
                  containers:
            """ + hardenedContainer("web", "shop/web:1.8.2", "") + """
            ---
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: orders
            spec:
              replicas: 2
              selector:
                matchLabels: {app: orders}
              template:
                metadata:
                  labels: {app: orders}
                spec:
                  containers:
            """ + hardenedContainer("orders", "shop/orders:3.1.0", """
                        env:
                          - name: DB_PASSWORD
                            valueFrom:
                              secretKeyRef: {name: orders-db, key: password}
                """) + """
            ---
            apiVersion: apps/v1
            kind: DaemonSet
            metadata:
              name: metrics-agent
            spec:
              selector:
                matchLabels: {app: metrics-agent}
              template:
                metadata:
                  labels: {app: metrics-agent}
                spec:
                  hostNetwork: true
                  containers:
            """ + hardenedContainer("agent", "shop/metrics-agent:2.4.1", "") + "\n";
}
