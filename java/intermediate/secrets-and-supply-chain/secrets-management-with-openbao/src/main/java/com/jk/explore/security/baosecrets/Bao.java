package com.jk.explore.security.baosecrets;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** A small client for OpenBao's HTTP API, with a token. */
final class Bao {

    static final JsonMapper JSON = JsonMapper.builder().build();

    record Reply(int status, Map<String, Object> json) {

        @SuppressWarnings("unchecked")
        Map<String, Object> data() {
            return (Map<String, Object>) json.get("data");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> auth() {
            return (Map<String, Object>) json.get("auth");
        }

        String string(String name) {
            return json.get(name) == null ? null : String.valueOf(json.get(name));
        }
    }

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final String base;
    private final String token;

    Bao(String base, String token) {
        this.base = base;
        this.token = token;
    }

    Bao withToken(String other) {
        return new Bao(base, other);
    }

    Reply get(String path) throws IOException, InterruptedException {
        return send("GET", path, null);
    }

    Reply put(String path, Object body) throws IOException, InterruptedException {
        return send("PUT", path, body);
    }

    Reply post(String path, Object body) throws IOException, InterruptedException {
        return send("POST", path, body);
    }

    Reply delete(String path) throws IOException, InterruptedException {
        return send("DELETE", path, null);
    }

    private Reply send(String method, String path, Object body) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + "/v1/" + path)).method(method,
                body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        if (token != null) {
            b.header("X-Vault-Token", token);
        }
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        Map<String, Object> json = r.body().isBlank() ? Map.of() : JSON.readValue(r.body(), new TypeReference<Map<String, Object>>() {
        });
        return new Reply(r.statusCode(), json);
    }
}
