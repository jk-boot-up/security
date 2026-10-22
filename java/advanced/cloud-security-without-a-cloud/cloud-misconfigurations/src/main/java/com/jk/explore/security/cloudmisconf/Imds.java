package com.jk.explore.security.cloudmisconf;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** The instance metadata service as a server on a loopback port, in the two versions, and the two web features that reach it. */
public final class Imds implements AutoCloseable {

    static final String ROLE_PATH = "/latest/meta-data/iam/security-credentials/web-role";
    static final String SECRET = "{\"AccessKeyId\":\"ASIAEXAMPLE\",\"SecretAccessKey\":\"example-secret\",\"Token\":\"example-session\"}";

    private final HttpServer server;
    private final Map<String, Boolean> tokens = new ConcurrentHashMap<>();

    public Imds(boolean tokenRequired) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", ex -> handle(ex, tokenRequired));
        server.start();
    }

    private void handle(HttpExchange ex, boolean tokenRequired) throws IOException {
        int status = 200;
        String body = "";
        if (ex.getRequestHeaders().containsKey("X-Forwarded-For")) {
            status = 403;
        } else if (ex.getRequestMethod().equals("PUT") && ex.getRequestURI().getPath().equals("/latest/api/token")) {
            String token = UUID.randomUUID().toString();
            tokens.put(token, true);
            body = token;
        } else if (ex.getRequestMethod().equals("GET") && ex.getRequestURI().getPath().equals(ROLE_PATH)) {
            String t = ex.getRequestHeaders().getFirst("X-aws-ec2-metadata-token");
            if (tokenRequired && (t == null || !tokens.containsKey(t))) {
                status = 401;
            } else {
                body = SECRET;
            }
        } else {
            status = 404;
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            ex.getResponseBody().write(bytes);
        }
        ex.close();
    }

    public String base() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    /** Result of a web feature that fetched something for a visitor. */
    public record Fetched(int status, String body) {
        public boolean leaked() {
            return body.contains("SecretAccessKey");
        }
    }

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    /** "Import a picture from a link": the server does a GET of any address it is given. */
    public static Fetched fetchUrl(String url) throws IOException, InterruptedException {
        HttpResponse<String> r = CLIENT.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
        return new Fetched(r.statusCode(), r.body());
    }

    /** A "webhook tester": the visitor chooses the method and one header, so a PUT with the token header is possible. */
    public static Fetched webhookTester(String method, String url, String headerName, String headerValue) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).method(method, HttpRequest.BodyPublishers.noBody());
        if (headerName != null) {
            b.header(headerName, headerValue);
        }
        HttpResponse<String> r = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Fetched(r.statusCode(), r.body());
    }
}
