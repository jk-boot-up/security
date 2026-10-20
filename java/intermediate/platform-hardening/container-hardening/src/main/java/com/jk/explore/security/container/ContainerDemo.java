package com.jk.explore.security.container;

import java.util.List;

/** Runs the container story in five numbered acts and prints exact, repeatable results. */
public class ContainerDemo {

    public static void main(String[] args) {
        System.out.print(run());
    }

    static ContainerSpec hardened() {
        return ContainerSpec.dockerDefaults().named("hardened").user("10001").dropAllCapabilities().readOnlyFilesystem().noNewPrivileges()
                .withLimits(512, 200).secretsAsFiles().withImage("shop-app@sha256:9f2c1a7d");
    }

    public static String run() {
        StringBuilder out = new StringBuilder();

        out.append("== 1. What 'docker run' with no options gives an attacker ==\n");
        List<Outcome> defaults = Attacker.run(ContainerSpec.dockerDefaults());
        defaults.forEach(o -> out.append(o).append('\n'));
        out.append(Attacker.possible(defaults)).append(" of ").append(Attacker.ATTACKS.size()).append(" attacks possible; level: ")
                .append(PodSecurityLevel.of(ContainerSpec.dockerDefaults())).append('\n');

        out.append("\n== 2. The configuration from the tutorial ==\n");
        ContainerSpec reckless = ContainerSpec.reckless();
        List<Outcome> bad = Attacker.run(reckless);
        out.append(Renderer.dockerFlags(reckless)).append('\n');
        out.append(Attacker.possible(bad)).append(" of ").append(Attacker.ATTACKS.size()).append(" attacks possible; level: ").append(PodSecurityLevel.of(reckless)).append('\n');
        PodSecurityLevel.violations(reckless, PodSecurityLevel.BASELINE).forEach(v -> out.append("  not baseline: ").append(v).append('\n'));

        out.append("\n== 3. One setting at a time ==\n");
        ContainerSpec c = ContainerSpec.reckless().named("reckless");
        step(out, "start: the tutorial configuration", c);
        c = c.withoutHostAccess();
        step(out, "no privileged, no host network, PID or mounts", c);
        c = c.withSeccomp(Seccomp.RUNTIME_DEFAULT);
        step(out, "default seccomp profile", c);
        c = c.dropAllCapabilities();
        step(out, "drop all capabilities", c);
        c = c.user("10001");
        step(out, "run as a user that is not root", c);
        c = c.noNewPrivileges();
        step(out, "no new privileges", c);
        c = c.readOnlyFilesystem();
        step(out, "read-only root, noexec temporary directory", c);
        c = c.withLimits(512, 200);
        step(out, "memory and process limits", c);

        out.append("\n== 4. The hardened container ==\n");
        ContainerSpec h = hardened();
        out.append(Renderer.dockerFlags(h)).append('\n');
        out.append(Renderer.kubernetesSecurityContext(h));
        out.append("level: ").append(PodSecurityLevel.of(h)).append('\n');
        Attacker.run(h).stream().filter(Outcome::possible).forEach(o -> out.append("  still possible: ").append(o).append('\n'));

        out.append("\n== 5. Settings that look harmless ==\n");
        ContainerSpec netRaw = h.addCapability("NET_RAW");
        out.append("hardened + NET_RAW -> level ").append(PodSecurityLevel.of(netRaw)).append(", ").append(Attacker.possible(Attacker.run(netRaw))).append(" of 13 attacks possible (NET_RAW matters only to root)\n");
        ContainerSpec admin = h.user("root").addCapability("SYS_ADMIN");
        out.append("hardened + root + SYS_ADMIN -> level ").append(PodSecurityLevel.of(admin)).append(", ").append(Attacker.possible(Attacker.run(admin))).append(" of 13 attacks possible\n");
        return out.toString();
    }

    private static void step(StringBuilder out, String label, ContainerSpec c) {
        List<Outcome> o = Attacker.run(c);
        out.append(String.format("%-48s -> %2d of %d possible, level %s%n", label, Attacker.possible(o), Attacker.ATTACKS.size(), PodSecurityLevel.of(c)));
    }
}
