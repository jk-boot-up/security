package com.jk.explore.security.apispring;

import java.util.stream.Stream;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** One API server on a free loopback port. Properties are command-line arguments, which beat the property files. */
record Running(ConfigurableApplicationContext context, String url) implements AutoCloseable {

    static Running start(String profile, RedisLab redis, String... properties) {
        Stream<String> redisArgs = redis == null ? Stream.empty()
                : Stream.of("spring.data.redis.port=" + redis.port(), "spring.data.redis.password=" + RedisLab.PASSWORD);
        String[] args = Stream.concat(redisArgs, Stream.of(properties)).map(p -> "--" + p).toArray(String[]::new);
        ConfigurableApplicationContext context = new SpringApplicationBuilder(ApiApp.class).profiles(profile).run(args);
        return new Running(context, "http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port"));
    }

    Client client() {
        return new Client(url);
    }

    <T> T bean(Class<T> type) {
        return context.getBean(type);
    }

    @Override
    public void close() {
        context.close();
    }
}
