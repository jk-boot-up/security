package com.jk.explore.security.runtimesec;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * The sensor that stands in for Falco on a kernel that cannot run Falco's driver: Linux audit, switched on by a privileged
 * container that shares the kernel's process view, copying the records of Kubernetes containers to a file we read back.
 */
public final class Sensor implements AutoCloseable {

    private final String name = "runtimesec-sensor-" + UUID.randomUUID().toString().substring(0, 6);

    public Sensor() throws IOException, InterruptedException {
        String script;
        try (InputStream in = Sensor.class.getResourceAsStream("/collector.sh")) {
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Sh.must(List.of("docker", "run", "-d", "--name", name, "--privileged", "--pid=host", "-e", "COLLECTOR=" + script,
                "alpine:3.22", "sh", "-c", "printf '%s\\n' \"$COLLECTOR\" > /collector.sh; exec sh /collector.sh"));
        for (int i = 0; i < 120; i++) {
            if (Sh.run("docker", "exec", name, "test", "-f", "/ready").ok()) {
                return;
            }
            Thread.sleep(1_000);
        }
        close();
        throw new IllegalStateException("the sensor did not start");
    }

    /** Everything copied so far. */
    public String log() throws IOException, InterruptedException {
        return Sh.must(List.of("docker", "exec", name, "cat", "/events.log"));
    }

    @Override
    public void close() {
        try {
            Sh.run("docker", "exec", name, "sh", "-c", "auditctl -D >/dev/null 2>&1; auditctl -e 0 >/dev/null 2>&1");
            Sh.run("docker", "rm", "-f", name);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
