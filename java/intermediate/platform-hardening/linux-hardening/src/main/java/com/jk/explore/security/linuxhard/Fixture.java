package com.jk.explore.security.linuxhard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** A prepared file system with the usual mistakes, made in a temporary directory: stands in for a container image mounted at a path. */
public final class Fixture implements AutoCloseable {

    private final Path root;

    private Fixture(Path root) {
        this.root = root;
    }

    public Path root() {
        return root;
    }

    public static Fixture insecure() throws IOException {
        Path r = Files.createTempDirectory("linux-hardening");
        Fixture fx = new Fixture(r);
        fx.write("etc/passwd", 0644, """
                root:x:0:0:root:/root:/bin/bash
                daemon:x:1:1:daemon:/usr/sbin:/usr/sbin/nologin
                toor:x:0:0:backdoor:/root:/bin/bash
                test:x:1001:1001:test:/home/test:/bin/bash
                ana:x:1002:1002:Ana:/home/ana:/bin/bash
                www:x:33:33:www:/var/www:/usr/sbin/nologin
                """);
        fx.write("etc/shadow", 0644, "root:$6$salt$hash:19000:0:99999:7:::\ntest::19000:0:99999:7:::\nana:$6$salt$hash2:19000:0:99999:7:::\ntoor:$6$salt$hash3:19000:0:99999:7:::\n");
        fx.write("etc/ssh/sshd_config", 0644, "# sshd\nPermitRootLogin yes\nPasswordAuthentication yes\nX11Forwarding yes\nPermitEmptyPasswords yes\n");
        fx.write("etc/ssh/ssh_host_ed25519_key", 0644, "-----BEGIN OPENSSH PRIVATE KEY----- (demo placeholder, not a key)\n");
        fx.write("etc/sysctl.conf", 0644, "kernel.randomize_va_space = 0\nnet.ipv4.conf.all.accept_redirects = 1\n");
        fx.write("etc/cron.daily/backup.sh", 0775, "#!/bin/sh\ntar czf /var/backups/site.tgz /var/www\n");
        fx.write("opt/app/config.properties", 0644, "db.password=demo-not-real\n");
        fx.write("opt/app/run.sh", 0755, "#!/bin/sh\nexec java -jar app.jar\n");
        fx.write("usr/bin/passwd", 04755, "binary");
        fx.write("usr/bin/sudo", 04755, "binary");
        fx.write("tmp/.hidden-shell", 04755, "binary");
        fx.write("var/www/uploads/readme.txt", 0666, "uploads\n");
        fx.dir("tmp", 01777);
        fx.dir("var/www/uploads", 0777);
        fx.write("proc/net/tcp", 0444, """
                  sl  local_address rem_address   st tx_queue rx_queue
                   0: 00000000:0016 00000000:0000 0A 00000000:00000000
                   1: 00000000:0017 00000000:0000 0A 00000000:00000000
                   2: 00000000:18EB 00000000:0000 0A 00000000:00000000
                   3: 0100007F:0CEA 00000000:0000 0A 00000000:00000000
                   4: 00000000:01BB 00000000:0000 0A 00000000:00000000
                   5: 00000000:0947 00000000:0000 0A 00000000:00000000
                """);
        return fx;
    }

    private void dir(String rel, int mode) throws IOException {
        Path d = root.resolve(rel);
        Files.createDirectories(d);
        Modes.set(d, mode);
    }

    private void write(String rel, int mode, String content) throws IOException {
        Path f = root.resolve(rel);
        Files.createDirectories(f.getParent());
        Files.writeString(f, content);
        Modes.set(f, mode);
    }

    /** The hardened configuration: what a person decides after reading the report. */
    public void hardenByHand() throws IOException {
        Files.writeString(root.resolve("etc/ssh/sshd_config"), "PermitRootLogin no\nPasswordAuthentication no\nX11Forwarding no\nPermitEmptyPasswords no\n");
        Files.writeString(root.resolve("etc/passwd"), """
                root:x:0:0:root:/root:/bin/bash
                daemon:x:1:1:daemon:/usr/sbin:/usr/sbin/nologin
                ana:x:1002:1002:Ana:/home/ana:/bin/bash
                www:x:33:33:www:/var/www:/usr/sbin/nologin
                """);
        Files.writeString(root.resolve("etc/shadow"), "root:$6$salt$hash:19000:0:99999:7:::\nana:$6$salt$hash2:19000:0:99999:7:::\n");
        Files.writeString(root.resolve("etc/sysctl.conf"), "kernel.randomize_va_space = 2\nnet.ipv4.tcp_syncookies = 1\nnet.ipv4.conf.all.accept_redirects = 0\n");
        Files.writeString(root.resolve("proc/net/tcp"), """
                  sl  local_address rem_address   st tx_queue rx_queue
                   0: 00000000:0016 00000000:0000 0A 00000000:00000000
                   1: 0100007F:0CEA 00000000:0000 0A 00000000:00000000
                   2: 00000000:01BB 00000000:0000 0A 00000000:00000000
                """);
        Files.deleteIfExists(root.resolve("tmp/.hidden-shell"));
    }

    @Override
    public void close() throws IOException {
        try (Stream<Path> all = Files.walk(root)) {
            for (Path p : (Iterable<Path>) all.sorted(Comparator.reverseOrder())::iterator) {
                if (Files.isDirectory(p) && !Files.isSymbolicLink(p)) {
                    Modes.set(p, 0755);
                }
                Files.deleteIfExists(p);
            }
        }
    }
}
