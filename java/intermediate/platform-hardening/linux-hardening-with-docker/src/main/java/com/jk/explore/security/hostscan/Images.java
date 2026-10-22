package com.jk.explore.security.hostscan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** The two images of the lesson, built from Dockerfiles written here: a typical one with the usual mistakes, and one that was hardened in its recipe. */
final class Images {

    static final String BASE = "alpine:3.24.3";
    static final String TYPICAL = "shop-host:typical";
    static final String HARDENED = "shop-host:hardened";

    private Images() {
    }

    static final String SSHD_TYPICAL = """
            Port 22
            PermitRootLogin yes
            PasswordAuthentication yes
            PermitEmptyPasswords yes
            X11Forwarding yes
            """;

    static final String SSHD_HARDENED = """
            Port 22
            PermitRootLogin no
            PasswordAuthentication no
            PermitEmptyPasswords no
            X11Forwarding no
            """;

    /** What accumulates in an image that nobody reviews: each line is a small, ordinary decision. */
    static void buildTypical() throws IOException, InterruptedException {
        String dockerfile = """
                FROM %s
                RUN adduser -D -s /bin/sh test && passwd -d test \\
                 && echo 'toor:x:0:0:Second root:/root:/bin/sh' >> /etc/passwd \\
                 && echo 'toor:!:19000:0:99999:7:::' >> /etc/shadow
                RUN mkdir -p /etc/ssh /var/www/uploads /opt/app /etc/cron.daily
                COPY sshd_config /etc/ssh/sshd_config
                RUN echo 'not a real key' > /etc/ssh/ssh_host_ed25519_key && chmod 644 /etc/ssh/ssh_host_ed25519_key
                RUN chmod 777 /var/www/uploads && echo hello > /var/www/uploads/readme.txt && chmod 666 /var/www/uploads/readme.txt
                RUN echo 'db.password=not-a-real-password' > /opt/app/config.properties && chmod 644 /opt/app/config.properties
                RUN printf '#!/bin/sh\\ntrue\\n' > /etc/cron.daily/backup.sh && chmod 775 /etc/cron.daily/backup.sh
                RUN cp /bin/busybox /tmp/.hidden-shell && chmod 4755 /tmp/.hidden-shell
                RUN chmod 644 /etc/shadow
                CMD ["sh", "-c", "for p in 23 2375 6379; do (while true; do nc -l -p $p; done) & done; sleep 100000"]
                """.formatted(BASE);
        build(TYPICAL, dockerfile, SSHD_TYPICAL);
    }

    /** The same shop with the decisions made in the recipe: no second root, no test account, a locked service account, tight modes, SSH settings, one listener. */
    static void buildHardened() throws IOException, InterruptedException {
        String dockerfile = """
                FROM %s
                RUN addgroup -S app && adduser -S -G app -s /sbin/nologin app
                RUN mkdir -p /etc/ssh /var/www/uploads /opt/app /etc/cron.daily
                COPY sshd_config /etc/ssh/sshd_config
                RUN echo 'not a real key' > /etc/ssh/ssh_host_ed25519_key && chmod 600 /etc/ssh/ssh_host_ed25519_key
                RUN echo hello > /var/www/uploads/readme.txt && chown -R app:app /var/www/uploads && chmod 750 /var/www/uploads && chmod 640 /var/www/uploads/readme.txt
                RUN echo 'db.password=not-a-real-password' > /opt/app/config.properties && chown app:app /opt/app/config.properties && chmod 600 /opt/app/config.properties
                RUN printf '#!/bin/sh\\ntrue\\n' > /etc/cron.daily/backup.sh && chmod 750 /etc/cron.daily/backup.sh
                USER app
                CMD ["sh", "-c", "while true; do nc -l -p 8080; done"]
                """.formatted(BASE);
        build(HARDENED, dockerfile, SSHD_HARDENED);
    }

    private static void build(String tag, String dockerfile, String sshd) throws IOException, InterruptedException {
        Path context = Files.createTempDirectory("hostscan-" + UUID.randomUUID().toString().substring(0, 6));
        Files.writeString(context.resolve("Dockerfile"), dockerfile);
        Files.writeString(context.resolve("sshd_config"), sshd);
        Docker.run("build", "-q", "-t", tag, context.toString());
    }
}
