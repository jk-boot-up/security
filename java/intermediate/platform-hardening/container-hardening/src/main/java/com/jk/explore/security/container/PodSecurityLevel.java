package com.jk.explore.security.container;

import java.util.ArrayList;
import java.util.List;

/** The three Kubernetes Pod Security Standards levels, strictest last. */
public enum PodSecurityLevel {
    PRIVILEGED, BASELINE, RESTRICTED;

    /** The highest level the container satisfies. */
    public static PodSecurityLevel of(ContainerSpec c) {
        if (!violations(c, BASELINE).isEmpty()) {
            return PRIVILEGED;
        }
        return violations(c, RESTRICTED).isEmpty() ? RESTRICTED : BASELINE;
    }

    /** What stands between this container and the given level. */
    public static List<String> violations(ContainerSpec c, PodSecurityLevel level) {
        List<String> v = new ArrayList<>();
        if (level == PRIVILEGED) {
            return v;
        }
        if (c.privileged()) {
            v.add("privileged container");
        }
        if (c.hostNetwork()) {
            v.add("host network");
        }
        if (c.hostPid()) {
            v.add("host PID namespace");
        }
        if (!c.hostMounts().isEmpty()) {
            v.add("host path volumes: " + String.join(", ", c.hostMounts()));
        }
        if (c.seccomp() == Seccomp.UNCONFINED) {
            v.add("seccomp unconfined");
        }
        c.capabilities().stream().sorted().filter(cap -> !ContainerSpec.DOCKER_DEFAULT_CAPABILITIES.contains(cap)).forEach(cap -> v.add("capability beyond the default set: " + cap));
        if (level == BASELINE) {
            return v;
        }
        if (c.runsAsRoot()) {
            v.add("runs as root");
        }
        if (c.allowPrivilegeEscalation()) {
            v.add("allowPrivilegeEscalation is true");
        }
        if (!c.capabilities().isEmpty()) {
            v.add("does not drop ALL capabilities");
        }
        return v;
    }
}
