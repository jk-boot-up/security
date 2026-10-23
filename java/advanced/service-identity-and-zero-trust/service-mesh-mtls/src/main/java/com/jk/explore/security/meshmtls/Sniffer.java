package com.jk.explore.security.meshmtls;

import java.io.IOException;
import java.util.UUID;

/** A packet capture run beside the cluster's node (sharing its network and process space), the way someone with root on the node would. */
final class Sniffer implements AutoCloseable {

    /** What to do while the capture runs. */
    interface Traffic {
        void run() throws Exception;
    }

    private final String name = "sniffer-" + UUID.randomUUID().toString().substring(0, 6);

    Sniffer(String node) throws IOException, InterruptedException {
        Sh.must(java.util.List.of("docker", "run", "-d", "--name", name, "--pid", "container:" + node, "--net", "container:" + node,
                "--privileged", "alpine:3", "sleep", "3600"));
        Sh.must(java.util.List.of("docker", "exec", name, "apk", "add", "-q", "tcpdump"));
    }

    /**
     * Captures while the traffic runs and returns the packets as text. With pid 0 it listens on every interface of the node;
     * with a pid it enters that process's network namespace (a pod's) and listens on the loopback inside it.
     */
    String capture(int pid, String filter, Traffic traffic) throws Exception {
        String listen = pid == 0 ? "tcpdump -l -A -n -i any '" + filter + "'" : "nsenter -t " + pid + " -n tcpdump -l -A -n -i lo '" + filter + "'";
        Sh.must(java.util.List.of("docker", "exec", name, "rm", "-f", "/tmp/cap.txt", "/tmp/err.txt"));
        Sh.must(java.util.List.of("docker", "exec", "-d", name, "sh", "-c", listen + " > /tmp/cap.txt 2> /tmp/err.txt"));
        for (int i = 0; i < 50; i++) {
            if (Sh.run("docker", "exec", name, "grep", "-q", "listening", "/tmp/err.txt").ok()) {
                break;
            }
            Thread.sleep(200);
        }
        traffic.run();
        Thread.sleep(2_000);
        Sh.run("docker", "exec", name, "pkill", "tcpdump");
        Thread.sleep(500);
        return Sh.must(java.util.List.of("docker", "exec", name, "cat", "/tmp/cap.txt"));
    }

    @Override
    public void close() {
        try {
            Sh.run("docker", "rm", "-f", name);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
