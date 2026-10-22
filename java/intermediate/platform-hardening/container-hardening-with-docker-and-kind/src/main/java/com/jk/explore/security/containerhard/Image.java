package com.jk.explore.security.containerhard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** The shop's application image, as an ordinary team would have built it: an app user that owns the app, and a secret in the environment. */
final class Image {

    static final String TAG = "shop-app:probe";

    private Image() {
    }

    static void build() throws IOException, InterruptedException {
        Path context = Files.createTempDirectory("containerhard-image");
        Files.writeString(context.resolve("Dockerfile"), """
                FROM alpine:3.24.3
                RUN adduser -S -u 10001 app && mkdir -p /app /hostetc && echo 'not a real jar' > /app/shop.jar && chown -R 10001 /app
                ENV DB_PASSWORD=not-a-real-password
                CMD ["sleep", "100000"]
                """);
        Sh.must(Sh.docker("build", "-q", "-t", TAG, context.toString()));
    }
}
