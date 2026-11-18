package com.jk.explore.security.container;

import java.util.List;
import java.util.function.Function;

/** The three Kubernetes Pod Security Standards levels, strictest last, and the checks that decide them. */
public enum PodSecurityLevel {
    PRIVILEGED, BASELINE, RESTRICTED;

    /** One check of a level: its name, and what a container that fails it gets wrong (empty when it passes). */
    public record Check(PodSecurityLevel level, String name, Function<ContainerSpec, List<String>> problems) {

        public boolean passes(ContainerSpec c) {
            return problems.apply(c).isEmpty();
        }
    }

    /** Baseline blocks the known ways out to the host; Restricted adds the rules for the process itself. */
    public static final List<Check> CHECKS = List.of(
            new Check(BASELINE, "not privileged", c -> c.privileged() ? List.of("privileged container") : List.of()),
            new Check(BASELINE, "no host network",
                    c -> c.hostNetwork() ? List.of("host network") : List.of()),
            new Check(BASELINE, "no host PID namespace", c -> c.hostPid() ? List.of("host PID namespace") : List.of()),
            new Check(BASELINE, "no host path volumes",
                    c -> c.hostMounts().isEmpty() ? List.of() : List.of("host path volumes: " + String.join(", ", c.hostMounts()))),
            new Check(BASELINE, "seccomp not unconfined", c -> c.seccomp() == Seccomp.UNCONFINED ? List.of("seccomp unconfined") : List.of()),
            new Check(BASELINE, "only default capabilities", c -> c.capabilities().stream().sorted()
                    .filter(cap -> !ContainerSpec.DOCKER_DEFAULT_CAPABILITIES.contains(cap))
                    .map(cap -> "capability beyond the default set: " + cap).toList()),
            new Check(RESTRICTED, "not root",
                    c -> c.runsAsRoot() ? List.of("runs as root") : List.of()),
            new Check(RESTRICTED, "no privilege escalation",
                    c -> c.allowPrivilegeEscalation() ? List.of("allowPrivilegeEscalation is true") : List.of()),
            new Check(RESTRICTED, "drops ALL capabilities",
                    c -> c.capabilities().isEmpty() ? List.of() : List.of("does not drop ALL capabilities")));

    /** The highest level the container satisfies. */
    public static PodSecurityLevel of(ContainerSpec c) {
        if (!violations(c, BASELINE).isEmpty()) {
            return PRIVILEGED;
        }
        return violations(c, RESTRICTED).isEmpty() ? RESTRICTED : BASELINE;
    }

    /** What stands between this container and the given level: the problems of every check up to that level. */
    public static List<String> violations(ContainerSpec c, PodSecurityLevel level) {
        return CHECKS.stream().filter(k -> k.level().ordinal() <= level.ordinal())
                .flatMap(k -> k.problems().apply(c).stream()).toList();
    }
}
