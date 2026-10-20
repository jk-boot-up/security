package com.jk.explore.security.container;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The settings of a container that matter when the process inside is taken over: who it runs as, what it may do to the kernel, what it can write, what of the host it can
 * see, and how much it may use. The names follow Docker's flags and Kubernetes' {@code securityContext}.
 */
public record ContainerSpec(String name, String user, boolean privileged, Set<String> capabilities, boolean readOnlyRootFilesystem, boolean tmpfsNoExec,
                            boolean allowPrivilegeEscalation, Seccomp seccomp, List<String> hostMounts, boolean hostNetwork, boolean hostPid,
                            long memoryLimitMb, int pidsLimit, boolean cpuLimit, boolean secretsInEnvironment, String image) {

    /** Docker's default capability set: what a container has when nobody says otherwise. */
    public static final Set<String> DOCKER_DEFAULT_CAPABILITIES = Set.of("CHOWN", "DAC_OVERRIDE", "FSETID", "FOWNER", "MKNOD", "NET_RAW", "SETGID", "SETUID", "SETFCAP",
            "SETPCAP", "NET_BIND_SERVICE", "SYS_CHROOT", "KILL", "AUDIT_WRITE");

    /** What {@code docker run shop-app} gives: root, 14 capabilities, the default seccomp profile, a writable file system and no limits. */
    public static ContainerSpec dockerDefaults() {
        return new ContainerSpec("docker run defaults", "root", false, DOCKER_DEFAULT_CAPABILITIES, false, false, true, Seccomp.RUNTIME_DEFAULT, List.of(), false, false,
                0, 0, false, true, "shop-app:latest");
    }

    /** The configuration found in many tutorials: privileged, the Docker socket and the host network, "to make it work". */
    public static ContainerSpec reckless() {
        return new ContainerSpec("reckless", "root", true, Set.of("ALL"), false, false, true, Seccomp.UNCONFINED, List.of("/var/run/docker.sock", "/etc"), true, true,
                0, 0, false, true, "shop-app:latest");
    }

    public ContainerSpec user(String u) {
        return new ContainerSpec(name, u, privileged, capabilities, readOnlyRootFilesystem, tmpfsNoExec, allowPrivilegeEscalation, seccomp, hostMounts, hostNetwork, hostPid,
                memoryLimitMb, pidsLimit, cpuLimit, secretsInEnvironment, image);
    }

    public ContainerSpec named(String n) {
        return new ContainerSpec(n, user, privileged, capabilities, readOnlyRootFilesystem, tmpfsNoExec, allowPrivilegeEscalation, seccomp, hostMounts, hostNetwork, hostPid,
                memoryLimitMb, pidsLimit, cpuLimit, secretsInEnvironment, image);
    }

    public ContainerSpec dropAllCapabilities() {
        return new ContainerSpec(name, user, privileged, Set.of(), readOnlyRootFilesystem, tmpfsNoExec, allowPrivilegeEscalation, seccomp, hostMounts, hostNetwork, hostPid,
                memoryLimitMb, pidsLimit, cpuLimit, secretsInEnvironment, image);
    }

    public ContainerSpec addCapability(String cap) {
        Set<String> c = new HashSet<>(capabilities);
        c.add(cap);
        return new ContainerSpec(name, user, privileged, Set.copyOf(c), readOnlyRootFilesystem, tmpfsNoExec, allowPrivilegeEscalation, seccomp, hostMounts, hostNetwork, hostPid,
                memoryLimitMb, pidsLimit, cpuLimit, secretsInEnvironment, image);
    }

    public ContainerSpec readOnlyFilesystem() {
        return new ContainerSpec(name, user, privileged, capabilities, true, true, allowPrivilegeEscalation, seccomp, hostMounts, hostNetwork, hostPid,
                memoryLimitMb, pidsLimit, cpuLimit, secretsInEnvironment, image);
    }

    public ContainerSpec noNewPrivileges() {
        return new ContainerSpec(name, user, privileged, capabilities, readOnlyRootFilesystem, tmpfsNoExec, false, seccomp, hostMounts, hostNetwork, hostPid,
                memoryLimitMb, pidsLimit, cpuLimit, secretsInEnvironment, image);
    }

    public ContainerSpec withLimits(long memoryMb, int pids) {
        return new ContainerSpec(name, user, privileged, capabilities, readOnlyRootFilesystem, tmpfsNoExec, allowPrivilegeEscalation, seccomp, hostMounts, hostNetwork, hostPid,
                memoryMb, pids, true, secretsInEnvironment, image);
    }

    public ContainerSpec withoutHostAccess() {
        return new ContainerSpec(name, user, false, capabilities, readOnlyRootFilesystem, tmpfsNoExec, allowPrivilegeEscalation, seccomp, List.of(), false, false,
                memoryLimitMb, pidsLimit, cpuLimit, secretsInEnvironment, image);
    }

    public ContainerSpec withSeccomp(Seccomp s) {
        return new ContainerSpec(name, user, privileged, capabilities, readOnlyRootFilesystem, tmpfsNoExec, allowPrivilegeEscalation, s, hostMounts, hostNetwork, hostPid,
                memoryLimitMb, pidsLimit, cpuLimit, secretsInEnvironment, image);
    }

    public ContainerSpec withImage(String i) {
        return new ContainerSpec(name, user, privileged, capabilities, readOnlyRootFilesystem, tmpfsNoExec, allowPrivilegeEscalation, seccomp, hostMounts, hostNetwork, hostPid,
                memoryLimitMb, pidsLimit, cpuLimit, secretsInEnvironment, i);
    }

    public ContainerSpec secretsAsFiles() {
        return new ContainerSpec(name, user, privileged, capabilities, readOnlyRootFilesystem, tmpfsNoExec, allowPrivilegeEscalation, seccomp, hostMounts, hostNetwork, hostPid,
                memoryLimitMb, pidsLimit, cpuLimit, false, image);
    }

    public boolean runsAsRoot() {
        return user.equals("root") || user.equals("0");
    }

    public boolean has(String capability) {
        return privileged || capabilities.contains("ALL") || capabilities.contains(capability);
    }
}
