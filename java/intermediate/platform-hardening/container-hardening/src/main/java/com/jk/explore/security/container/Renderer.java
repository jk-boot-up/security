package com.jk.explore.security.container;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes the settings as Docker flags, as a Kubernetes container {@code securityContext} and as a short summary, so the checked configuration is the one that is
 * deployed.
 */
public final class Renderer {

    private Renderer() {
    }

    /** The {@code docker run} arguments, one flag (with its value) per entry, the image last. */
    public static List<String> dockerArgs(ContainerSpec c) {
        List<String> a = new ArrayList<>();
        if (c.privileged()) {
            a.add("--privileged");
        }
        if (!c.runsAsRoot()) {
            a.add("--user " + c.user());
        }
        if (c.capabilities().isEmpty()) {
            a.add("--cap-drop=ALL");
        }
        if (c.readOnlyRootFilesystem()) {
            a.add("--read-only");
            a.add("--tmpfs /tmp:rw,noexec,nosuid");
        }
        if (!c.allowPrivilegeEscalation()) {
            a.add("--security-opt no-new-privileges");
        }
        if (c.seccomp() == Seccomp.UNCONFINED) {
            a.add("--security-opt seccomp=unconfined");
        }
        if (c.memoryLimitMb() > 0) {
            a.add("--memory " + c.memoryLimitMb() + "m");
        }
        if (c.pidsLimit() > 0) {
            a.add("--pids-limit " + c.pidsLimit());
        }
        if (c.hostNetwork()) {
            a.add("--network host");
        }
        if (c.hostPid()) {
            a.add("--pid host");
        }
        c.hostMounts().forEach(m -> a.add("-v " + m + ":" + m));
        a.add(c.image());
        return a;
    }

    /** The command on one line. */
    public static String dockerFlags(ContainerSpec c) {
        return "docker run " + String.join(" ", dockerArgs(c));
    }

    /** The command one flag per line, as a shell script would write it. */
    public static List<String> dockerLines(ContainerSpec c) {
        List<String> args = dockerArgs(c);
        List<String> lines = new ArrayList<>();
        lines.add("docker run \\");
        for (int i = 0; i < args.size(); i++) {
            lines.add("  " + args.get(i) + (i < args.size() - 1 ? " \\" : ""));
        }
        return lines;
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

    /** The settings that decide the attacks, as {@code name, value} pairs a person can read. */
    public static List<String[]> settings(ContainerSpec c) {
        List<String> host = new ArrayList<>();
        if (c.hostNetwork()) {
            host.add("network");
        }
        if (c.hostPid()) {
            host.add("PID");
        }
        if (!c.hostMounts().isEmpty()) {
            host.add(c.hostMounts().size() + " mount(s)");
        }
        String caps = c.privileged() || c.capabilities().contains("ALL") ? "all of them"
                : c.capabilities().isEmpty() ? "none (all dropped)"
                : c.capabilities().equals(ContainerSpec.DOCKER_DEFAULT_CAPABILITIES) ? c.capabilities().size() + ", Docker's default set"
                : String.join(", ", c.capabilities().stream().sorted().toList());
        return List.of(
                new String[] {"user", c.runsAsRoot() ? "root" : c.user() + " (not root)"},
                new String[] {"privileged", c.privileged() ? "yes" : "no"},
                new String[] {"capabilities", caps},
                new String[] {"root files", c.readOnlyRootFilesystem() ? "read-only" : "writable"},
                new String[] {"new privileges", c.allowPrivilegeEscalation() ? "allowed" : "forbidden"},
                new String[] {"seccomp", c.seccomp() == Seccomp.RUNTIME_DEFAULT ? "RuntimeDefault" : "Unconfined"},
                new String[] {"host access", host.isEmpty() ? "none" : String.join(", ", host)},
                new String[] {"limits", c.memoryLimitMb() == 0 && c.pidsLimit() == 0 ? "none"
                        : c.memoryLimitMb() + " MB, " + c.pidsLimit() + " processes"},
                new String[] {"secrets", c.secretsInEnvironment() ? "in the environment" : "as files"});
    }
}
