package com.jk.explore.security.hostscan;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** A running container from one of the images, removed on close. */
final class Host implements AutoCloseable {

    private final String name = "host-" + UUID.randomUUID().toString().substring(0, 8);

    /** @param sysctls namespaced kernel settings the container is started with, such as net.ipv4.tcp_syncookies=0 */
    Host(String image, List<String> sysctls) throws IOException, InterruptedException {
        List<String> command = new java.util.ArrayList<>(List.of("run", "-d", "--rm", "--name", name));
        for (String s : sysctls) {
            command.addAll(List.of("--sysctl", s));
        }
        command.add(image);
        Docker.run(command.toArray(String[]::new));
        for (int i = 0; i < 50; i++) {
            if (Docker.run("inspect", "-f", "{{.State.Running}}", name).strip().equals("true") && !Docker.exec(name, "cat /proc/net/tcp").lines().skip(1).findAny().isEmpty()) {
                return;
            }
            Thread.sleep(100);
        }
    }

    String name() {
        return name;
    }

    String exec(String script) throws IOException, InterruptedException {
        return Docker.exec(name, script);
    }

    @Override
    public void close() {
        try {
            Docker.run("rm", "-f", name);
        } catch (IOException | InterruptedException | IllegalStateException e) {
            // already gone
        }
    }
}
