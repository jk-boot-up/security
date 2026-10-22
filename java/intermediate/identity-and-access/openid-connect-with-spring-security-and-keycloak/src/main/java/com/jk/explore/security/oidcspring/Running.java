package com.jk.explore.security.oidcspring;

import java.io.IOException;
import java.net.ServerSocket;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** The store, a Spring Boot application on a loopback port that was chosen before Keycloak started, signing shoppers in with the bank and the wallet. */
record Running(ConfigurableApplicationContext context, String url) implements AutoCloseable {

    /** A free loopback port, found by opening one and closing it again. */
    static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    static Running start(KeycloakLab keycloak, int port, String... more) {
        String[] base = new String[] {"--server.port=" + port, "--store.bank-issuer=" + keycloak.issuer("bank"),
            registration("bank", keycloak), "--spring.security.oauth2.client.provider.bank.issuer-uri=" + keycloak.issuer("bank"),
            registration("wallet", keycloak), "--spring.security.oauth2.client.provider.wallet.issuer-uri=" + keycloak.issuer("wallet"),
            "--spring.security.oauth2.client.registration.bank.client-id=store-web", "--spring.security.oauth2.client.registration.wallet.client-id=store-web",
            "--spring.security.oauth2.client.registration.bank.client-secret=demo-store-secret", "--spring.security.oauth2.client.registration.wallet.client-secret=demo-store-secret",
            "--spring.security.oauth2.client.registration.bank.scope=openid,email", "--spring.security.oauth2.client.registration.wallet.scope=openid,email"};
        String[] args = java.util.stream.Stream.concat(java.util.Arrays.stream(base), java.util.Arrays.stream(more)).toArray(String[]::new);
        ConfigurableApplicationContext context = new SpringApplicationBuilder(StoreApp.class).run(args);
        return new Running(context, "http://127.0.0.1:" + port);
    }

    private static String registration(String id, KeycloakLab keycloak) {
        return "--spring.security.oauth2.client.registration." + id + ".provider=" + id;
    }

    <T> T bean(Class<T> type) {
        return context.getBean(type);
    }

    @Override
    public void close() {
        context.close();
    }
}
