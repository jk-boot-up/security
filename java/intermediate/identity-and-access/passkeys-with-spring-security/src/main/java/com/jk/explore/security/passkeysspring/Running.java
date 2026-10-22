package com.jk.explore.security.passkeysspring;

import java.util.stream.Stream;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.web.webauthn.management.MapPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.MapUserCredentialRepository;

/** The bank on a free loopback port. Properties are command-line arguments, which beat the property file. */
record Running(ConfigurableApplicationContext context, String url) implements AutoCloseable {

    static Running start(String... properties) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(PasskeysApp.class).run(Stream.of(properties).map(p -> "--" + p).toArray(String[]::new));
        return new Running(context, "http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port"));
    }

    PasskeyBrowser browser() {
        return new PasskeyBrowser(url);
    }

    MapUserCredentialRepository credentials() {
        return context.getBean(MapUserCredentialRepository.class);
    }

    MapPublicKeyCredentialUserEntityRepository users() {
        return context.getBean(MapPublicKeyCredentialUserEntityRepository.class);
    }

    @Override
    public void close() {
        context.close();
    }
}
