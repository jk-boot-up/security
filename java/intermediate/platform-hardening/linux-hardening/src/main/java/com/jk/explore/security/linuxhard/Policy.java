package com.jk.explore.security.linuxhard;

import java.util.Map;
import java.util.Set;

/** What this host is allowed to have. A scan with no policy would flag the system's own tools. */
public record Policy(Set<String> setuidAllowed, Set<Integer> portsAllowed, Map<String, Integer> maxModes, Set<String> defaultAccountNames) {

    public static Policy standard() {
        return new Policy(
                Set.of("/usr/bin/passwd", "/usr/bin/sudo", "/usr/bin/su"),
                Set.of(22, 443),
                Map.of("/etc/shadow", 0640, "/etc/ssh/ssh_host_ed25519_key", 0600, "/opt/app/config.properties", 0600, "/etc/passwd", 0644),
                Set.of("test", "guest", "admin", "oracle", "pi", "user", "ubuntu"));
    }
}
