package com.jk.explore.security.sessionredis;

import java.util.stream.Stream;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;

/** One bank server on a free loopback port, with its sessions in the given Redis. Properties are command-line arguments, which beat the property file. */
record Running(ConfigurableApplicationContext context, String url) implements AutoCloseable {

    static Running start(RedisLab redis, String... properties) {
        String[] args = Stream.concat(Stream.of("spring.data.redis.port=" + redis.port(), "spring.data.redis.password=" + RedisLab.PASSWORD), Stream.of(properties))
                .map(p -> "--" + p).toArray(String[]::new);
        ConfigurableApplicationContext context = new SpringApplicationBuilder(BankApp.class).run(args);
        return new Running(context, "http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port"));
    }

    Browser browser() {
        return new Browser(url);
    }

    <T> T bean(Class<T> type) {
        return context.getBean(type);
    }

    StringRedisTemplate redis() {
        return context.getBean(StringRedisTemplate.class);
    }

    @Override
    public void close() {
        context.close();
    }
}
