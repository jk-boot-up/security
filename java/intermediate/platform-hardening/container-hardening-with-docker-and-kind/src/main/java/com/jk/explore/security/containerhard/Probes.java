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
            """;

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
