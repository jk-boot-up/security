package com.jk.explore.security.oauthkeycloak;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** The accounts API, a resource server on a free loopback port that trusts the bank realm of the given Keycloak. */
record Running(ConfigurableApplicationContext context, String url) implements AutoCloseable {

    /** What the API answered: the status and the error the Bearer challenge names, if any. */
    record Reply(int status, String body, String challenge) {

        /** For example {@code 403 insufficient_scope}. */
        String text() {
            String error = challenge == null ? null : java.util.regex.Pattern.compile("error=\"([^\"]+)\"").matcher(challenge).results().map(m -> m.group(1)).findFirst().orElse(null);
            return status + (error == null ? "" : " " + error);
        }
    }

    static Running start(KeycloakLab keycloak) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(ApiApp.class).run("--bank.issuer=" + keycloak.issuer());
        return new Running(context, "http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port"));
    }

    OffsetClock clock() {
        return context.getBean(OffsetClock.class);
    }

    Reply get(String path, String accessToken) throws IOException, InterruptedException {
        return send(request(path, accessToken).GET().build());
    }

    Reply post(String path, String accessToken) throws IOException, InterruptedException {
        return send(request(path, accessToken).POST(HttpRequest.BodyPublishers.noBody()).build());
    }

    private HttpRequest.Builder request(String path, String accessToken) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url + path));
        if (accessToken != null) {
            b.header("Authorization", "Bearer " + accessToken);
        }
        return b;
    }

    private static Reply send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> r = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return new Reply(r.statusCode(), r.body(), r.headers().firstValue("WWW-Authenticate").orElse(null));
    }

    @Override
    public void close() {
        context.close();
    }
}
