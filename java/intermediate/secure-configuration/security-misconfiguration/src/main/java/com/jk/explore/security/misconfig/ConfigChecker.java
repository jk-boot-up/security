package com.jk.explore.security.misconfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Checks a running store from the outside, the way a scanner or an attacker would, and its settings file from the inside.
 * One point for each check passed.
 */
final class ConfigChecker {

    record Check(String name, boolean passed, String detail) {
    }

    private final HttpClient client = HttpClient.newHttpClient();
    private final String base;

    ConfigChecker(int port) {
        this.base = "http://127.0.0.1:" + port;
    }

    record Reply(int status, String body, Optional<String> server) {
    }

    Reply send(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path));
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/x-www-form-urlencoded").method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body(), response.headers().firstValue("Server"));
    }

    List<Check> checkAll(StoreConfig config, Path configFile) throws IOException, InterruptedException {
        List<Check> checks = new ArrayList<>();
        Reply login = send("POST", "/admin/login", "password=admin");
        checks.add(new Check("the default admin password does not work", login.status() != 200, "POST /admin/login with admin: " + login.status()));
        Reply debug = send("GET", "/debug/config", null);
        checks.add(new Check("no debug page", debug.status() == 404, "GET /debug/config: " + debug.status()));
        Reply listing = send("GET", "/static/", null);
        checks.add(new Check("no directory listing", listing.status() != 200, "GET /static/: " + listing.status()));
        Reply error = send("GET", "/orders?id=abc", null);
        boolean trace = error.body().contains("Exception") || error.body().contains("\tat ");
        checks.add(new Check("errors show no internals", !trace, "GET /orders?id=abc shows a stack trace: " + trace));
        Reply home = send("GET", "/", null);
        boolean banner = home.server().filter(s -> s.matches(".*\\d.*")).isPresent();
        checks.add(new Check("no product or version in the Server header", !banner, "Server: " + home.server().orElse("none")));
        Reply traceMethod = send("TRACE", "/", null);
        checks.add(new Check("TRACE is refused", traceMethod.status() == 405, "TRACE /: " + traceMethod.status()));
        boolean others = ConfigFile.readableByOthers(configFile);
        checks.add(new Check("settings file readable by its owner only", !others, "permissions " + ConfigFile.permissions(configFile)));
        boolean prodClean = "prod".equals(config.profile()) && config.testSettings().isEmpty();
        checks.add(new Check("production profile with no test settings", prodClean, "profile " + config.profile() + ", test settings " + config.testSettings().size()));
        return checks;
    }

    static int score(List<Check> checks) {
        return (int) checks.stream().filter(Check::passed).count();
    }
}
