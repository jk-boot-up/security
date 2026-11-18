package com.jk.explore.security.containerhard;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thirteen things a hijacked process tries, written as shell commands that run inside the container as whatever user the container runs as. Each prints
 * its name and POSSIBLE or blocked. They are real attempts: write to a file, mount, read a cgroup limit, open a socket.
 */
public final class Probes {

    /** The attacks, in the order they are printed. */
    public static final String[] NAMES = {"read-secrets", "tamper-with-app", "drop-and-run", "become-root", "dangerous-syscalls", "sniff-network", "mount-host", "docker-socket", "host-files", "host-network",
            "see-host-processes", "exhaust-memory", "fork-bomb"};

    static final String SCRIPT = """
            r() { if "$@" >/dev/null 2>&1; then echo "$N=POSSIBLE"; else echo "$N=blocked"; fi; }
            N=read-secrets; r test -n "$DB_PASSWORD"
            N=tamper-with-app; r sh -c 'echo x >> /app/shop.jar'
            N=drop-and-run; r sh -c 'printf "#!/bin/sh\\ntrue\\n" > /tmp/probe.sh && chmod +x /tmp/probe.sh && /tmp/probe.sh'
            N=become-root; r sh -c '[ "$(id -u)" = 0 ]'
            N=dangerous-syscalls; r sh -c '[ "$(grep Seccomp: /proc/self/status | cut -f2)" = 0 ]'
            N=sniff-network; r sh -c 'cap=$(grep CapEff /proc/self/status | cut -f2); [ $(( 0x$cap >> 13 & 1 )) = 1 ]'
            N=mount-host; r mount -t tmpfs tmpfs /mnt
            N=docker-socket; r test -S /var/run/docker.sock
            N=host-files; r test -e /hostetc/passwd
            N=host-network; r sh -c 'grep -qE "docker0|veth" /proc/net/dev'
            N=see-host-processes; r sh -c '[ "$(ls /proc | grep -c "^[0-9]")" -gt 15 ]'
            N=exhaust-memory; r sh -c '[ "$(cat /sys/fs/cgroup/memory.max)" = max ]'
            N=fork-bomb; r sh -c '[ "$(cat /sys/fs/cgroup/pids.max)" = max ]'
            echo "fact id-u=$(id -u)"
            echo "fact CapEff=$(grep CapEff /proc/self/status | cut -f2)"
            echo "fact Seccomp=$(grep Seccomp: /proc/self/status | cut -f2)"
            echo "fact memory.max=$(cat /sys/fs/cgroup/memory.max)"
            echo "fact pids.max=$(cat /sys/fs/cgroup/pids.max)"
            """;

    public static final String CONTAINER = "inside the container";
    public static final String KERNEL = "the kernel";
    public static final String HOST = "the host";
    public static final String RESOURCES = "the node's resources";

    /** What each attack reaches, in the order the groups are printed. */
    public static final Map<String, String> REACH = new LinkedHashMap<>();

    static {
        for (String n : NAMES) {
            REACH.put(n, switch (n) {
                case "read-secrets", "tamper-with-app", "drop-and-run", "become-root" -> CONTAINER;
                case "dangerous-syscalls", "sniff-network", "mount-host" -> KERNEL;
                case "exhaust-memory", "fork-bomb" -> RESOURCES;
                default -> HOST;
            });
        }
    }

    /** What the probe saw, in a few words: why it worked, or why it did not. */
    public static String reason(String name, boolean possible) {
        return switch (name) {
            case "read-secrets" -> possible ? "DB_PASSWORD is in the env" : "no secret in the env";
            case "tamper-with-app" -> possible ? "/app/shop.jar is writable" : "/app/shop.jar not writable";
            case "drop-and-run" -> possible ? "a script in /tmp runs" : "a script in /tmp cannot run";
            case "become-root" -> possible ? "id -u is 0" : "id -u is not 0";
            case "dangerous-syscalls" -> possible ? "Seccomp is 0: no filter" : "Seccomp is 2: a filter is on";
            case "sniff-network" -> possible ? "the NET_RAW bit is set" : "the NET_RAW bit is clear";
            case "mount-host" -> possible ? "mount tmpfs worked" : "mount tmpfs was refused";
            case "docker-socket" -> possible ? "the socket is mounted" : "no Docker socket";
            case "host-files" -> possible ? "/hostetc/passwd is there" : "no host directory mounted";
            case "host-network" -> possible ? "it sees docker0 or veth" : "its own network namespace";
            case "see-host-processes" -> possible ? "more than 15 processes" : "its own process namespace";
            case "exhaust-memory" -> possible ? "memory.max is max" : "memory.max is set";
            case "fork-bomb" -> possible ? "pids.max is max" : "pids.max is set";
            default -> throw new IllegalArgumentException(name);
        };
    }

    /** The {@code fact name=value} lines of the script's output: what the probes read. */
    public static Map<String, String> facts(String output) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String line : output.split("\n")) {
            if (line.startsWith("fact ") && line.indexOf('=') > 5) {
                out.put(line.substring(5, line.indexOf('=')), line.substring(line.indexOf('=') + 1).strip());
            }
        }
        return out;
    }

    private Probes() {
    }

    /** Reads the script's output: attack name to whether it worked. */
    public static Map<String, Boolean> parse(String output) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        for (String line : output.split("\n")) {
            int eq = line.indexOf('=');
            if (eq > 0 && (line.endsWith("=POSSIBLE") || line.endsWith("=blocked"))) {
                out.put(line.substring(0, eq), line.endsWith("POSSIBLE"));
            }
        }
        return out;
    }

    public static long possible(Map<String, Boolean> results) {
        return results.values().stream().filter(b -> b).count();
    }

    public static java.util.List<String> possibleNames(Map<String, Boolean> results) {
        return results.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
    }
}
