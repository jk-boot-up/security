package com.jk.explore.security.containerhard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** One container started with a list of docker run options, probed, and removed. */
public final class Run {

    private Run() {
    }

    /** Starts the shop image with {@code options}, runs the thirteen attempts inside it, and removes it. */
    public static Map<String, Boolean> probe(List<String> options) throws IOException, InterruptedException {
        String name = "probe-" + UUID.randomUUID().toString().substring(0, 8);
        List<String> command = new ArrayList<>(List.of("docker", "run", "-d", "--rm", "--name", name));
        command.addAll(options);
        command.add(Image.TAG);
        try {
            Sh.must(command);
            Thread.sleep(300);
            return Probes.parse(Sh.must(List.of("docker", "exec", name, "sh", "-c", Probes.SCRIPT)));
        } finally {
            Sh.run("docker", "rm", "-f", name);
        }
    }
}
