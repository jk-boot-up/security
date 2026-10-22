package com.jk.explore.security.apispring;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** An app that calls the bank's API with a customer's credentials in a header. */
final class Client {

    record Reply(int status, String body, HttpResponse<String> raw) {

        String header(String name) {
            return raw.headers().firstValue(name).orElse(null);
        }

        /** The names of the fields in a JSON object, in the order the bank sent them. */
        List<String> fields() {
            return json().keySet().stream().toList();
        }

        Map<String, Object> json() {
            return MAPPER.readValue(body, new TypeReference<Map<String, Object>>() {
            });
        }

        /** The objects in a JSON array. */
        List<Map<String, Object>> list() {
            return MAPPER.readValue(body, new TypeReference<List<Map<String, Object>>>() {
            });
        }

        /** The problem detail of a refusal. */
        String detail() {
            return String.valueOf(json().get("detail"));
        }
    }

    static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final String baseUrl;

    Client(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    Client at(String otherBaseUrl) {
        return new Client(otherBaseUrl);
    }

    Reply get(String path, String user) throws IOException, InterruptedException {
        return send(builder(path, user).GET().build());
    }

    Reply patch(String path, String user, String json) throws IOException, InterruptedException {
        return send(builder(path, user).header("Content-Type", "application/json").method("PATCH", HttpRequest.BodyPublishers.ofString(json)).build());
    }

    private HttpRequest.Builder builder(String path, String user) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path));
        if (user != null) {
            b.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((user + ":" + ApiSecurity.PASSWORD).getBytes(StandardCharsets.UTF_8)));
        }
        return b;
    }

    private Reply send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> r = http.send(request, HttpResponse.BodyHandlers.ofString());
        return new Reply(r.statusCode(), r.body(), r);
    }
}
