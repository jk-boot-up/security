package com.jk.explore.security.container;

import java.util.ArrayList;
import java.util.List;

/**
 * "What can a process do once it is taken over?" For each attack, a rule that says whether this container's settings allow it. The rules are the documented effects of
 * the Linux settings they name (capabilities, user, seccomp, mounts, namespaces, cgroups), not a simulation of the kernel.
 */
public final class Attacker {

    public static final String CONTAINER = "inside the container";
    public static final String KERNEL = "the kernel";
    public static final String HOST = "the host";
    public static final String RESOURCES = "the node's resources";

    public static final List<Attack> ATTACKS = List.of(
            new Attack("read-secrets", "read credentials the application holds", CONTAINER),
            new Attack("tamper-with-app", "change the application's own files", CONTAINER),
            new Attack("drop-and-run", "write a tool to disk and run it", CONTAINER),
            new Attack("become-root", "gain root inside the container", CONTAINER),
            new Attack("dangerous-syscalls", "use mount, ptrace, keyctl or module loading", KERNEL),
            new Attack("sniff-network", "open raw sockets on the pod network", KERNEL),
            new Attack("mount-host", "mount or load things as root in the kernel", KERNEL),
            new Attack("docker-socket", "start a privileged container through the Docker API", HOST),
            new Attack("host-files", "read or write host files through a mount", HOST),
            new Attack("host-network", "reach services bound to the host's network", HOST),
            new Attack("see-host-processes", "see and signal the host's processes", HOST),
            new Attack("exhaust-memory", "use all the host's memory", RESOURCES),
            new Attack("fork-bomb", "create processes until the host stops", RESOURCES));

    private Attacker() {
    }

    public static List<Outcome> run(ContainerSpec c) {
        List<Outcome> out = new ArrayList<>();
        for (Attack a : ATTACKS) {
            out.add(evaluate(a, c));
        }
        return out;
    }

    /** The Docker API socket: whoever can write to it can start a privileged container, which is root on the host. */
    public static final String DOCKER_SOCKET = "/var/run/docker.sock";

    /** The ids of the possible attacks, in the order of {@link #ATTACKS}. */
    public static List<String> possibleIds(ContainerSpec c) {
        return run(c).stream().filter(Outcome::possible).map(o -> o.attack().id()).toList();
    }

    public static long possible(List<Outcome> outcomes) {
        return outcomes.stream().filter(Outcome::possible).count();
    }

    private static Outcome evaluate(Attack a, ContainerSpec c) {
        return switch (a.id()) {
            case "read-secrets" -> c.secretsInEnvironment() ? yes(a, "secrets in the environment")
                    : yes(a, "the app must read its secrets");
            case "tamper-with-app" -> c.readOnlyRootFilesystem() ? no(a, "root file system read-only")
                    : yes(a, "root file system writable");
            case "drop-and-run" -> c.readOnlyRootFilesystem() && c.tmpfsNoExec() ? no(a, "read-only root, noexec /tmp")
                    : yes(a, c.readOnlyRootFilesystem() ? "/tmp allows execution" : "root file system writable");
            case "become-root" -> c.runsAsRoot() ? yes(a, "it already runs as root")
                    : c.allowPrivilegeEscalation() ? yes(a, "set-user-id programs work")
                    : no(a, "not root, no_new_privs set");
            case "dangerous-syscalls" -> c.privileged() || c.seccomp() == Seccomp.UNCONFINED ? yes(a, "no seccomp filter")
                    : no(a, "default seccomp blocks them");
            case "sniff-network" -> c.has("NET_RAW") && (c.runsAsRoot() || c.privileged()) ? yes(a, "root with NET_RAW")
                    : no(a, c.has("NET_RAW") ? "NET_RAW, but not root" : "NET_RAW dropped");
            case "mount-host" -> c.privileged() ? yes(a, "privileged: every capability")
                    : c.has("SYS_ADMIN") && c.runsAsRoot() ? yes(a, "root with SYS_ADMIN")
                    : no(a, "no SYS_ADMIN");
            case "docker-socket" -> c.hostMounts().contains(DOCKER_SOCKET) ? yes(a, "Docker socket mounted")
                    : no(a, "no Docker socket");
            case "host-files" -> c.hostMounts().isEmpty() ? no(a, "no host directories mounted")
                    : yes(a, c.hostMounts().size() + " host path(s) mounted");
            case "host-network" -> c.hostNetwork() ? yes(a, "shares the host's network") : no(a, "its own network namespace");
            case "see-host-processes" -> c.hostPid() ? yes(a, "shares the host's processes") : no(a, "its own process namespace");
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
