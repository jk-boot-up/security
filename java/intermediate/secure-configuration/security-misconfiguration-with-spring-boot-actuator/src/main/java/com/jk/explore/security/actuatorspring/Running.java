package com.jk.explore.security.actuatorspring;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** A store started on free loopback ports with the given profile. Extra properties are given as command-line arguments, which beat the profile's file. Closing it stops the servers. */
record Running(ConfigurableApplicationContext context, String customerUrl, String managementUrl) implements AutoCloseable {

    static Running start(String profile, String... properties) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(StoreApp.class).profiles(profile)
                .run(java.util.Arrays.stream(properties).map(p -> "--" + p).toArray(String[]::new));
        String customer = "http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port");
        String management = "http://127.0.0.1:" + context.getEnvironment().getProperty("local.management.port");
        return new Running(context, customer, management);
    }

    /** True when operations share the customers' port, which is the shipped arrangement. */
    boolean sharesPort() {
        return customerUrl.equals(managementUrl);
    }

    @Override
    public void close() {
        context.close();
    }
}
