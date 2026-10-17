package com.jk.explore.security.authspring;

import jakarta.servlet.Filter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * One running copy of the shop: a Spring application context holding Spring Security's filter chain,
 * driven by MockMvc, so requests go through the real chain without opening a network port.
 */
public class Shop implements AutoCloseable {

    /** What a request got back. */
    public record Result(int status, String body) {
        @Override
        public String toString() {
            return switch (status) {
                case 200 -> "200 OK";
                case 401 -> "401 UNAUTHENTICATED";
                case 403 -> "403 FORBIDDEN";
                default -> String.valueOf(status);
            };
        }
    }

    private final AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    private final MockMvc mvc;

    public Shop(Class<?> config) {
        context.setServletContext(new MockServletContext());
        context.register(config);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    public CountingPasswordEncoder encoder() {
        return context.getBean(CountingPasswordEncoder.class);
    }

    /** Sends a request. A null user sends no credentials at all. */
    public Result send(HttpMethod method, String path, String user, String password) {
        try {
            var request = MockMvcRequestBuilders.request(method, path);
            if (user != null) {
                String pair = user + ":" + password;
                request.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8)));
            }
            var response = mvc.perform(request).andReturn().getResponse();
            return new Result(response.getStatus(), response.getContentAsString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Sends a request as one of the four accounts, whose password is the name followed by -pass. */
    public Result as(String user, HttpMethod method, String path) {
        return send(method, path, user, user + "-pass");
    }

    @Override
    public void close() {
        context.close();
    }
}
