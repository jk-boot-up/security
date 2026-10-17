package com.jk.explore.security.authkeycloak;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** The order API started on a free loopback port with the given token decoder. Closing it stops the server. */
record Running(ConfigurableApplicationContext context, int port) implements AutoCloseable {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    static Running start(JwtDecoder decoder) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(Shop.class)
                .initializers(c -> c.getBeanFactory().registerSingleton("jwtDecoder", decoder))
                .properties("server.port=0", "server.address=127.0.0.1", "logging.level.root=OFF", "spring.main.banner-mode=off")
                .run();
        return new Running(context, Integer.parseInt(context.getEnvironment().getProperty("local.server.port")));
    }

    /** The status code for a GET, with an optional bearer token. */
    int status(String path, String token) throws IOException, InterruptedException {
        return get(path, token).statusCode();
    }

    HttpResponse<String> get(String path, String token) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Override
    public void close() {
        context.close();
    }
}
