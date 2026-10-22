package com.jk.explore.security.failclosedspring;

import java.io.IOException;
import java.util.stream.Stream;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** A bank and its fraud service, both on free loopback ports. Extra properties are command-line arguments, which beat the property files. */
record Running(ConfigurableApplicationContext context, FraudServer fraud, String url) implements AutoCloseable {

    static Running start(String profile, String... properties) throws IOException {
        FraudServer fraud = new FraudServer();
        String[] args = Stream.concat(Stream.of("bank.fraud.url=" + fraud.url()), Stream.of(properties)).map(p -> "--" + p).toArray(String[]::new);
        ConfigurableApplicationContext context = new SpringApplicationBuilder(BankApp.class).profiles(profile).run(args);
        return new Running(context, fraud, "http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port"));
    }

    <T> T bean(Class<T> type) {
        return context.getBean(type);
    }

    @Override
    public void close() {
        context.close();
        fraud.close();
    }
}
