package com.jk.explore.security.container;

import java.util.ArrayList;
import java.util.List;

/**
 * "What can a process do once it is taken over?" For each attack, a rule that says whether this container's settings allow it. The rules are the documented effects of
 * the Linux settings they name (capabilities, user, seccomp, mounts, namespaces, cgroups), not a simulation of the kernel.
 */
public final class Attacker {

    public static final List<Attack> ATTACKS = List.of(
            new Attack("read-secrets", "read credentials the application holds"),
            new Attack("tamper-with-app", "change the application's own files"),
            new Attack("drop-and-run", "write a tool to disk and run it"),
            new Attack("become-root", "gain root inside the container"),
            new Attack("dangerous-syscalls", "use mount, ptrace, keyctl or module loading"),
            new Attack("sniff-network", "open raw sockets on the pod network"),
            new Attack("mount-host", "mount or load things as root in the kernel"),
            new Attack("docker-socket", "start a privileged container through the Docker API"),
            new Attack("host-files", "read or write host files through a mount"),
            new Attack("host-network", "reach services bound to the host's network"),
            new Attack("see-host-processes", "see and signal the host's processes"),
            new Attack("exhaust-memory", "use all the host's memory"),
            new Attack("fork-bomb", "create processes until the host stops"));

    private Attacker() {
    }

    public static List<Outcome> run(ContainerSpec c) {
        List<Outcome> out = new ArrayList<>();
        for (Attack a : ATTACKS) {
            out.add(evaluate(a, c));
        }
        return out;
    }

    public static long possible(List<Outcome> outcomes) {
        return outcomes.stream().filter(Outcome::possible).count();
    }

    private static Outcome evaluate(Attack a, ContainerSpec c) {
        return switch (a.id()) {
            case "read-secrets" -> c.secretsInEnvironment()
                    ? yes(a, "secrets are in the environment, which every process of the container can read")
                    : yes(a, "the application must read its secrets, so a hijacked application can too");
            case "tamper-with-app" -> c.readOnlyRootFilesystem() ? no(a, "the root file system is read-only") : yes(a, "the root file system is writable");
            case "drop-and-run" -> c.readOnlyRootFilesystem() && c.tmpfsNoExec() ? no(a, "read-only root and a noexec temporary directory")
                    : yes(a, c.readOnlyRootFilesystem() ? "the temporary directory allows execution" : "the root file system is writable");
            case "become-root" -> c.runsAsRoot() ? yes(a, "it already runs as root")
                    : c.allowPrivilegeEscalation() ? yes(a, "set-user-id programs in the image can raise privileges")
                    : no(a, "not root, and no_new_privs forbids gaining privileges");
            case "dangerous-syscalls" -> c.privileged() || c.seccomp() == Seccomp.UNCONFINED ? yes(a, "no seccomp filter")
                    : no(a, "the default seccomp profile blocks them");
            case "sniff-network" -> c.has("NET_RAW") && (c.runsAsRoot() || c.privileged()) ? yes(a, "root with the NET_RAW capability")
                    : no(a, c.has("NET_RAW") ? "NET_RAW is present but the user is not root" : "the NET_RAW capability is dropped");
            case "mount-host" -> c.privileged() ? yes(a, "a privileged container has every capability and device")
                    : c.has("SYS_ADMIN") && c.runsAsRoot() ? yes(a, "root with CAP_SYS_ADMIN")
                    : no(a, "no CAP_SYS_ADMIN");
            case "docker-socket" -> c.hostMounts().contains("/var/run/docker.sock") ? yes(a, "the Docker socket is mounted: whoever can write to it is root on the host")
                    : no(a, "the Docker socket is not mounted");
            case "host-files" -> c.hostMounts().isEmpty() ? no(a, "no host directories are mounted") : yes(a, "mounted: " + String.join(", ", c.hostMounts()));
            case "host-network" -> c.hostNetwork() ? yes(a, "the container shares the host's network namespace") : no(a, "its own network namespace");
            case "see-host-processes" -> c.hostPid() ? yes(a, "the container shares the host's process namespace") : no(a, "its own process namespace");
            case "exhaust-memory" -> c.memoryLimitMb() == 0 ? yes(a, "no memory limit") : no(a, "limited to " + c.memoryLimitMb() + " MB");
            case "fork-bomb" -> c.pidsLimit() == 0 ? yes(a, "no process limit") : no(a, "limited to " + c.pidsLimit() + " processes");
            default -> throw new IllegalArgumentException(a.id());
        };
    }

    private static Outcome yes(Attack a, String why) {
        return new Outcome(a, true, why);
    }

    private static Outcome no(Attack a, String why) {
        return new Outcome(a, false, why);
    }
}
