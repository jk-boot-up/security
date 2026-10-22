package com.jk.explore.security.oidcspring;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A shopper's browser as far as this lesson needs one: it keeps cookies for each address separately, follows redirects one hop at a time and remembers where it went,
 * and when Keycloak shows its sign-in page it types the shopper's user name and password there. Keycloak marks its cookies Secure and a browser still sends them to
 * 127.0.0.1 over http, which Java's cookie manager does not, so the jar is kept here.
 */
final class Shopper {

    /** Where the browser ended up, and every address it passed on the way. */
    record Page(int status, String url, String body, List<String> hops) {
    }

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NEVER).build();
    private final Map<String, Map<String, String>> jars = new LinkedHashMap<>();

    Page navigate(String url, String user, String password) throws IOException, InterruptedException {
        return navigate(url, user, password, null);
    }

    /** Like {@link #navigate(String, String, String)}, but stops before requesting an address that starts with {@code stopBefore} and returns that address. */
    Page navigate(String url, String user, String password, String stopBefore) throws IOException, InterruptedException {
        List<String> hops = new ArrayList<>();
        String current = url;
        for (int i = 0; i < 25; i++) {
            hops.add(current);
            if (stopBefore != null && current.startsWith(stopBefore)) {
                return new Page(302, current, "", hops);
            }
            HttpResponse<String> r = send(HttpRequest.newBuilder(URI.create(current)));
            if (r.statusCode() / 100 == 3) {
                current = URI.create(current).resolve(r.headers().firstValue("Location").orElseThrow()).toString();
                continue;
            }
            Matcher form = Pattern.compile("id=\"kc-form-login\"[^>]*action=\"([^\"]+)\"").matcher(r.body());
            if (form.find() && user != null) {
                String action = form.group(1).replace("&amp;", "&");
                hops.add(action);
                HttpResponse<String> submitted = send(HttpRequest.newBuilder(URI.create(action)).header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString("username=" + enc(user) + "&password=" + enc(password) + "&credentialId=")));
                if (submitted.statusCode() / 100 == 3) {
                    current = URI.create(action).resolve(submitted.headers().firstValue("Location").orElseThrow()).toString();
                    continue;
                }
                return new Page(submitted.statusCode(), action, submitted.body(), hops);
            }
            return new Page(r.statusCode(), current, r.body(), hops);
        }
        throw new IllegalStateException("too many redirects: " + hops);
    }

    Page get(String url) throws IOException, InterruptedException {
        return navigate(url, null, null);
    }

    Page post(String url, String form) throws IOException, InterruptedException {
        HttpResponse<String> r = send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)));
        String location = r.headers().firstValue("Location").orElse(null);
        return new Page(r.statusCode(), location == null ? url : URI.create(url).resolve(location).toString(), r.body(), new ArrayList<>(List.of(url)));
    }

    /** One request with no redirects followed, so the demo can read the address a redirect names. */
    HttpResponse<String> once(String url) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(URI.create(url)));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        URI uri = request.build().uri();
        String origin = uri.getHost() + ":" + uri.getPort();
        Map<String, String> jar = jars.computeIfAbsent(origin, o -> new LinkedHashMap<>());
        if (!jar.isEmpty()) {
            StringBuilder cookie = new StringBuilder();
            jar.forEach((k, v) -> cookie.append(cookie.length() == 0 ? "" : "; ").append(k).append('=').append(v));
            request.header("Cookie", cookie.toString());
        }
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        for (String header : response.headers().allValues("Set-Cookie")) {
            String pair = header.split(";", 2)[0];
            int eq = pair.indexOf('=');
            String value = pair.substring(eq + 1);
            if (value.isEmpty()) {
                jar.remove(pair.substring(0, eq));
            } else {
                jar.put(pair.substring(0, eq), value);
            }
        }
        return response;
    }

    static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
