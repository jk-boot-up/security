package com.jk.explore.security.samlkeycloak;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** The shop, a Spring Boot application on a loopback port chosen before Keycloak started, trusting the identity provider of one Keycloak realm. */
record Running(ConfigurableApplicationContext context, String url) implements AutoCloseable {

    static final String ENTITY_PATH = "/saml2/service-provider-metadata/keycloak";
    static final String ACS_PATH = "/login/saml2/sso/keycloak";

    static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    /** Starts the shop for the given realm's identity provider, reading its signing certificate from the realm's metadata. */
    static Running start(KeycloakLab keycloak, int port, String realm, String... more) {
        String registration = "--spring.security.saml2.relyingparty.registration.keycloak.";
        String[] base = {"--server.port=" + port, registration + "entity-id=http://127.0.0.1:" + port + ENTITY_PATH,
            registration + "assertingparty.metadata-uri=" + keycloak.metadata(realm), registration + "assertingparty.singlesignon.sign-request=false"};
        String[] args = java.util.stream.Stream.concat(java.util.Arrays.stream(base), java.util.Arrays.stream(more)).toArray(String[]::new);
        ConfigurableApplicationContext context = new SpringApplicationBuilder(ShopApp.class).run(args);
        return new Running(context, "http://127.0.0.1:" + port);
    }

    @Override
    public void close() {
        context.close();
    }
}
