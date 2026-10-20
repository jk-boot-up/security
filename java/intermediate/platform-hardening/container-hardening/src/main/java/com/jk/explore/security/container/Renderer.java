package com.jk.explore.security.container;

import java.util.stream.Collectors;

/** Writes the settings as Docker flags and as a Kubernetes container {@code securityContext}, so the checked configuration is the one that is deployed. */
public final class Renderer {

    private Renderer() {
    }

    public static String dockerFlags(ContainerSpec c) {
        StringBuilder b = new StringBuilder("docker run");
        if (c.privileged()) {
            b.append(" --privileged");
        }
        if (!c.runsAsRoot()) {
            b.append(" --user ").append(c.user());
        }
        if (c.capabilities().isEmpty()) {
            b.append(" --cap-drop=ALL");
        }
        if (c.readOnlyRootFilesystem()) {
            b.append(" --read-only --tmpfs /tmp:rw,noexec,nosuid");
        }
        if (!c.allowPrivilegeEscalation()) {
            b.append(" --security-opt no-new-privileges");
        }
        if (c.seccomp() == Seccomp.UNCONFINED) {
            b.append(" --security-opt seccomp=unconfined");
        }
        if (c.memoryLimitMb() > 0) {
            b.append(" --memory ").append(c.memoryLimitMb()).append('m');
        }
        if (c.pidsLimit() > 0) {
            b.append(" --pids-limit ").append(c.pidsLimit());
        }
        if (c.hostNetwork()) {
            b.append(" --network host");
        }
        if (c.hostPid()) {
            b.append(" --pid host");
        }
        c.hostMounts().forEach(m -> b.append(" -v ").append(m).append(':').append(m));
        return b.append(' ').append(c.image()).toString();
    }

    public static String kubernetesSecurityContext(ContainerSpec c) {
        StringBuilder b = new StringBuilder("securityContext:\n");
        b.append("  privileged: ").append(c.privileged()).append('\n');
        b.append("  runAsNonRoot: ").append(!c.runsAsRoot()).append('\n');
        if (!c.runsAsRoot()) {
            b.append("  runAsUser: ").append(c.user()).append('\n');
        }
        b.append("  allowPrivilegeEscalation: ").append(c.allowPrivilegeEscalation()).append('\n');
        b.append("  readOnlyRootFilesystem: ").append(c.readOnlyRootFilesystem()).append('\n');
        b.append("  capabilities:\n    drop: [").append(c.capabilities().isEmpty() ? "ALL" : "").append("]\n");
        if (!c.capabilities().isEmpty()) {
            b.append("    add: [").append(c.capabilities().stream().sorted().collect(Collectors.joining(", "))).append("]\n");
        }
        b.append("  seccompProfile:\n    type: ").append(c.seccomp() == Seccomp.RUNTIME_DEFAULT ? "RuntimeDefault" : "Unconfined").append('\n');
        return b.toString();
    }
}
