package com.jk.explore.security.corsspring;

import java.util.stream.Stream;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** A bank started on a free loopback port with the given CORS policy. Closing it stops the server. */
record Running(ConfigurableApplicationContext context, String url) implements AutoCloseable {

    static Running start(String mode, String where, String... properties) {
        String[] args = Stream.concat(Stream.of("bank.cors.mode=" + mode, "bank.cors.where=" + where), Stream.of(properties)).map(p -> "--" + p).toArray(String[]::new);
        ConfigurableApplicationContext context = new SpringApplicationBuilder(BankApp.class).run(args);
        return new Running(context, "http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port"));
    }

    Processed processed() {
        return context.getBean(Processed.class);
    }

    Browser browser() {
        return new Browser(url, processed());
    }

    @Override
    public void close() {
        context.close();
    }
}
