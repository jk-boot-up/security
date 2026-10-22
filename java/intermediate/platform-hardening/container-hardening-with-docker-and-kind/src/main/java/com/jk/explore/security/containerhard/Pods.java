package com.jk.explore.security.containerhard;

/** The three pods of the lesson as manifests: the tutorial's, the default, and the hardened one. */
final class Pods {

    private Pods() {
    }

    private static final String HEADER = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: %s
            spec:
            """;

    /** What the tutorial asks for: privileged, the host's network and processes, and the host's Docker socket. */
    static String tutorial(String name) {
        return HEADER.formatted(name) + """
                  hostNetwork: true
                  hostPID: true
                  volumes:
                  - name: sock
                    hostPath: {path: /var/run/docker.sock}
                  containers:
                  - name: app
                    image: %s
                    imagePullPolicy: Never
                    command: ["sleep", "100000"]
                    securityContext: {privileged: true}
                    volumeMounts: [{name: sock, mountPath: /var/run/docker.sock}]
                """.formatted(Image.TAG);
    }

    /** A pod written the way the first example in a tutorial writes it: an image and a command. */
    static String plain(String name) {
        return HEADER.formatted(name) + """
                  containers:
                  - name: app
                    image: %s
                    imagePullPolicy: Never
                    command: ["sleep", "100000"]
                """.formatted(Image.TAG);
    }

    /** The securityContext of the plain project, with a memory limit and a temporary directory. */
    static String hardened(String name) {
        return HEADER.formatted(name) + """
                  securityContext:
                    runAsNonRoot: true
                    runAsUser: 10001
                    seccompProfile: {type: RuntimeDefault}
                  containers:
                  - name: app
                    image: %s
                    imagePullPolicy: Never
                    command: ["sleep", "100000"]
                    securityContext:
                      allowPrivilegeEscalation: false
                      readOnlyRootFilesystem: true
                      capabilities: {drop: [ALL]}
                    resources: {limits: {memory: 256Mi}}
                    volumeMounts: [{name: tmp, mountPath: /tmp}]
                  volumes:
                  - name: tmp
                    emptyDir: {medium: Memory}
                """.formatted(Image.TAG);
    }
}
