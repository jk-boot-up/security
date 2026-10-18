package com.jk.explore.security.oauth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The same authorization server and API behind a real HTTP server on the loopback interface. The authorization endpoint takes the
 * signed-in user and the consent answer as two extra parameters, standing in for the bank's sign-in and consent pages.
 */
final class LoopbackBank implements AutoCloseable {

    private final HttpServer http;
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    LoopbackBank(AuthorizationServer server, BankApi api) throws IOException {
        http = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        http.createContext("/authorize", exchange -> {
            Map<String, String> params = Urls.parse(exchange.getRequestURI().getRawQuery());
            AuthorizationServer.AuthorizeResult result = server.authorize(params, params.get("user"), "yes".equals(params.get("consent")));
            if (result.redirected()) {
                exchange.getResponseHeaders().add("Location", result.redirectTo());
                reply(exchange, 302, "");
            } else {
                reply(exchange, 400, result.errorPage());
            }
        });
        http.createContext("/token", exchange -> {
            Map<String, String> form = Urls.parse(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.getResponseHeaders().add("Cache-Control", "no-store");
            try {
                AuthorizationServer.Tokens tokens = server.token(form);
                reply(exchange, 200, "{\"access_token\":\"" + tokens.accessToken() + "\",\"token_type\":\"Bearer\",\"expires_in\":"
                        + tokens.expiresIn() + ",\"refresh_token\":\"" + tokens.refreshToken() + "\",\"scope\":\"" + String.join(" ", tokens.scope()) + "\"}");
            } catch (OAuthException e) {
                reply(exchange, e.error().equals("invalid_client") ? 401 : 400, "{\"error\":\"" + e.error() + "\"}");
            }
        });
        http.createContext("/accounts", exchange -> {
            BankApi.Answer answer = api.call(exchange.getRequestMethod(), "/accounts", exchange.getRequestHeaders().getFirst("Authorization"));
            reply(exchange, answer.status(), answer.body());
        });
        http.start();
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    private String base() {
        return "http://" + http.getAddress().getHostString() + ":" + http.getAddress().getPort();
    }

    /** The browser visits the authorization endpoint: returns the status and the Location header. */
    HttpResponse<String> authorize(String authorizeUrl, String user, boolean consent) throws IOException, InterruptedException {
        String query = URI.create(authorizeUrl).getRawQuery() + "&user=" + user + "&consent=" + (consent ? "yes" : "no");
        return client.send(HttpRequest.newBuilder(URI.create(base() + "/authorize?" + query)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    /** The client calls the token endpoint over HTTP. */
    AuthorizationServer.Tokens token(Map<String, String> form) throws OAuthException {
        try {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(base() + "/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(Urls.form(form))).build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new OAuthException(field(response.body(), "error"), "the token endpoint answered " + response.statusCode());
            }
            return new AuthorizationServer.Tokens(field(response.body(), "access_token"), field(response.body(), "refresh_token"),
                    Long.parseLong(number(response.body(), "expires_in")), Set.copyOf(AuthorizationServer.scopes(field(response.body(), "scope"))));
        } catch (IOException e) {
            throw new OAuthException("server_error", "the token endpoint could not be reached");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OAuthException("server_error", "interrupted");
        }
    }

    /** The client calls the API with its access token. Returns the status code. */
    int accounts(String accessToken) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(URI.create(base() + "/accounts")).header("Authorization", "Bearer " + accessToken).GET().build(),
                HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\":\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private static String number(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\":(\\d+)").matcher(json);
        return m.find() ? m.group(1) : "0";
    }

    static Map<String, String> locationQuery(HttpResponse<String> response) {
        return response.headers().firstValue("Location").map(Urls::queryOf).orElse(new LinkedHashMap<>());
    }

    @Override
    public void close() {
        http.stop(0);
    }
}
