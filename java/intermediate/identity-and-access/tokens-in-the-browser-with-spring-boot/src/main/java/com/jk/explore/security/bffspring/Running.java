package com.jk.explore.security.bffspring;

import com.jk.explore.security.bffspring.api.ApiApp;
import com.jk.explore.security.bffspring.bff.BffApp;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** A Spring Boot application on a loopback port: the accounts API or the backend for the frontend. */
public record Running(ConfigurableApplicationContext context, String url) implements AutoCloseable {

    /** A free loopback port, found by opening one and closing it again. */
    public static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    public static Running api(KeycloakLab keycloak) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(ApiApp.class).run("--server.port=0", "--bank.issuer=" + keycloak.issuer());
        return new Running(context, "http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port"));
    }

    /** The backend for the frontend on the port that the realm file names, calling the given API. */
    public static Running bff(KeycloakLab keycloak, int port, String apiUrl) {
        String registration = "--spring.security.oauth2.client.registration.keycloak.";
        ConfigurableApplicationContext context = new SpringApplicationBuilder(BffApp.class).run("--server.port=" + port, "--bff.api-url=" + apiUrl,
                registration + "client-id=bank-bff", registration + "client-secret=demo-bff-secret", registration + "scope=openid,accounts.read,payments.write",
                registration + "provider=keycloak", "--spring.security.oauth2.client.provider.keycloak.issuer-uri=" + keycloak.issuer(),
                "--server.servlet.session.cookie.name=__Host-bff", "--server.servlet.session.cookie.path=/", "--server.servlet.session.cookie.secure=true",
                "--server.servlet.session.cookie.http-only=true", "--server.servlet.session.cookie.same-site=strict");
        return new Running(context, "http://127.0.0.1:" + port);
    }

    public <T> T bean(Class<T> type) {
        return context.getBean(type);
    }

    @Override
    public void close() {
        context.close();
    }
}
