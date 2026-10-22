package com.jk.explore.security.tlsaudit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** The real testssl.sh, pointed at one server on a loopback port with the name a browser would use. */
final class TestSsl {

    private final Path root;
    private final Path work;

    /** @param root the lab's root, added to the roots testssl trusts */
    TestSsl(Path root, Path work) throws IOException {
        this.root = root;
        this.work = Files.createDirectories(work);
    }

    /** Scans protocols, cipher categories, the certificate and the HTTP headers, connecting to 127.0.0.1 and using {@code name} as the name a browser would. */
    Report scan(String name, int port) throws IOException, InterruptedException {
        return run(name, port, List.of("--ip", "127.0.0.1", name + ":" + port));
    }

    /** The same scan, started with the address instead of the name. */
    Report scanByAddress(int port) throws IOException, InterruptedException {
        return run("address", port, List.of("127.0.0.1:" + port));
    }

    private Report run(String name, int port, List<String> target) throws IOException, InterruptedException {
        Path json = work.resolve(name + "-" + port + ".json");
        Files.deleteIfExists(json);
        List<String> command = new java.util.ArrayList<>(List.of("testssl.sh", "--jsonfile", json.toString(), "--color", "0", "--warnings", "off", "--add-ca", root.toString(), "-p", "-s", "-S", "-h"));
        command.addAll(target);
        Process p = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(work.resolve(name + "-" + port + ".txt").toFile()).start();
        if (!p.waitFor(300, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IllegalStateException("testssl.sh did not finish for " + name + ":" + port);
        }
        return Report.parse(Files.readString(json));
    }
}
