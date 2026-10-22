package com.jk.explore.security.samlkeycloak;

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
 * A customer's browser as far as this lesson needs one. It keeps cookies for each address together with the attributes that decide who may see them: a cookie that is
 * HttpOnly is never shown to a script, and a cookie with SameSite=Strict is not sent on a request that another site started. It follows redirects one hop at a time, and when
 * Keycloak shows its sign-in page it types the user name and password there. Keycloak marks its cookies Secure and a browser still sends them to 127.0.0.1 over http,
 * which Java's cookie manager does not, so the jar is kept here.
 */
public final class Shopper {

    private record Cookie(String value, boolean httpOnly, String sameSite, String raw) {
    }

    /** What came back: the status, the body, where a redirect points, and the raw Set-Cookie headers. */
    public record Reply(int status, String body, String location, List<String> setCookies, Map<String, String> headers) {
    }

    /** Where the browser ended up after following redirects, and every address it passed. */
    public record Page(int status, String url, String body, List<String> hops) {
    }

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NEVER).build();
    private final Map<String, Map<String, Cookie>> jars = new LinkedHashMap<>();

    public Page navigate(String url, String user, String password) throws IOException, InterruptedException {
        return navigate(url, user, password, null);
    }

    /** Like the plain version, but stops before requesting an address that starts with {@code stopBefore} and returns that address. */
    public Page navigate(String url, String user, String password, String stopBefore) throws IOException, InterruptedException {
        return navigate("GET", url, null, user, password, stopBefore);
    }

    /** Starts with a form post, as the browser does when a page submits a SAML request by itself. */
    public Page navigatePost(String url, String form, String user, String password, String stopBefore) throws IOException, InterruptedException {
        return navigate("POST", url, form, user, password, stopBefore);
    }

    private Page navigate(String firstMethod, String url, String firstBody, String user, String password, String stopBefore) throws IOException, InterruptedException {
        List<String> hops = new ArrayList<>();
        String current = url;
        for (int i = 0; i < 25; i++) {
            hops.add(current);
            if (stopBefore != null && current.startsWith(stopBefore)) {
                return new Page(302, current, "", hops);
            }
            Reply r = i == 0 && firstMethod.equals("POST") ? request("POST", current, Map.of("Content-Type", "application/x-www-form-urlencoded"), firstBody, false) : request("GET", current, Map.of(), null, false);
            if (r.status() / 100 == 3) {
                current = URI.create(current).resolve(r.location()).toString();
                continue;
            }
            Matcher samlForm = Pattern.compile("(?is)<form[^>]*method=\"post\"[^>]*action=\"([^\"]+)\"[^>]*>.*?name=\"SAML(?:Request|Response)\"").matcher(r.body());
            if (samlForm.find() && !r.body().contains("kc-form-login")) {
                String action = samlForm.group(1).replace("&amp;", "&");
                if (stopBefore != null && action.startsWith(stopBefore)) {
                    return new Page(200, action, r.body(), hops);
                }
                hops.add(action);
                Reply posted = request("POST", action, Map.of("Content-Type", "application/x-www-form-urlencoded"), formBody(hiddenFields(r.body())), false);
                if (posted.status() / 100 == 3) {
                    current = URI.create(action).resolve(posted.location()).toString();
                    continue;
                }
                return new Page(posted.status(), action, posted.body(), hops);
            }
            Matcher form = Pattern.compile("id=\"kc-form-login\"[^>]*action=\"([^\"]+)\"").matcher(r.body());
            if (form.find() && user != null) {
                String action = form.group(1).replace("&amp;", "&");
                hops.add(action);
                Reply submitted = request("POST", action, Map.of("Content-Type", "application/x-www-form-urlencoded"),
                        "username=" + enc(user) + "&password=" + enc(password) + "&credentialId=", false);
                if (submitted.status() / 100 == 3) {
                    current = URI.create(action).resolve(submitted.location()).toString();
                    continue;
                }
                return new Page(submitted.status(), action, submitted.body(), hops);
            }
            return new Page(r.status(), current, r.body(), hops);
        }
        throw new IllegalStateException("too many redirects: " + hops);
    }

    public Reply get(String url) throws IOException, InterruptedException {
        return request("GET", url, Map.of(), null, false);
    }

    /**
     * One request with no redirects followed. When {@code startedByAnotherSite} is set, the request was caused by a page on another site, and the browser leaves out
     * the cookies that have a SameSite attribute.
     */
    public Reply request(String method, String url, Map<String, String> headers, String body, boolean startedByAnotherSite) throws IOException, InterruptedException {
        URI uri = URI.create(url);
        HttpRequest.Builder b = HttpRequest.newBuilder(uri).method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        headers.forEach(b::header);
        Map<String, Cookie> jar = jars.computeIfAbsent(origin(uri), o -> new LinkedHashMap<>());
        StringBuilder cookie = new StringBuilder();
        jar.forEach((k, c) -> {
            if (!(startedByAnotherSite && c.sameSite() != null)) {
                cookie.append(cookie.length() == 0 ? "" : "; ").append(k).append('=').append(c.value());
            }
        });
        if (cookie.length() > 0) {
            b.header("Cookie", cookie.toString());
        }
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        List<String> set = r.headers().allValues("Set-Cookie");
        for (String header : set) {
            String[] parts = header.split(";");
            int eq = parts[0].indexOf('=');
            String name = parts[0].substring(0, eq);
            String value = parts[0].substring(eq + 1);
            boolean httpOnly = false;
            String sameSite = null;
            for (int i = 1; i < parts.length; i++) {
                String attribute = parts[i].strip();
                if (attribute.equalsIgnoreCase("HttpOnly")) {
                    httpOnly = true;
                } else if (attribute.toLowerCase().startsWith("samesite=")) {
                    sameSite = attribute.substring(9);
                }
            }
            if (startedByAnotherSite && sameSite != null) {
                continue;   // a browser does not accept a SameSite cookie from a response to a request that another site started
            }
            if (value.isEmpty()) {
                jar.remove(name);
            } else {
                jar.put(name, new Cookie(value, httpOnly, sameSite, header));
            }
        }
        Map<String, String> responseHeaders = new LinkedHashMap<>();
        r.headers().map().forEach((k, v) -> responseHeaders.put(k.toLowerCase(), String.join(", ", v)));
        return new Reply(r.statusCode(), r.body(), r.headers().firstValue("Location").orElse(null), set, responseHeaders);
    }

    /** What a script on a page of this address can read from {@code document.cookie}: the cookies that are not HttpOnly. */
    public Map<String, String> documentCookie(String url) {
        Map<String, String> readable = new LinkedHashMap<>();
        jars.getOrDefault(origin(URI.create(url)), Map.of()).forEach((k, c) -> {
            if (!c.httpOnly()) {
                readable.put(k, c.value());
            }
        });
        return readable;
    }

    /** The Set-Cookie header that set the cookie, for looking at its attributes. */
    public String setCookieHeader(String url, String name) {
        Cookie c = jars.getOrDefault(origin(URI.create(url)), Map.of()).get(name);
        return c == null ? null : c.raw();
    }

    /** The value of a cookie, for the demo to play the part of a load balancer that moves the browser's cookie to another server. */
    public String cookieValue(String url, String name) {
        Cookie c = jars.getOrDefault(origin(URI.create(url)), Map.of()).get(name);
        return c == null ? null : c.value();
    }

    /** The names of every cookie the browser holds for this address, scripts or not. */
    public List<String> cookieNames(String url) {
        return List.copyOf(jars.getOrDefault(origin(URI.create(url)), Map.of()).keySet());
    }

    private static String origin(URI uri) {
        return uri.getHost() + ":" + uri.getPort();
    }

    /** The hidden fields of the page's one form, which is what the browser sends when the page submits it by itself. */
    public static Map<String, String> hiddenFields(String html) {
        Map<String, String> fields = new LinkedHashMap<>();
        Matcher m = Pattern.compile("(?is)<input[^>]*name=\"([^\"]+)\"[^>]*value=\"([^\"]*)\"|<input[^>]*value=\"([^\"]*)\"[^>]*name=\"([^\"]+)\"").matcher(html);
        while (m.find()) {
            if (m.group(1) != null) {
                fields.put(m.group(1), unescape(m.group(2)));
            } else {
                fields.put(m.group(4), unescape(m.group(3)));
            }
        }
        return fields;
    }

    private static String unescape(String value) {
        return value.replace("&#x3d;", "=").replace("&#61;", "=").replace("&#x2b;", "+").replace("&#43;", "+").replace("&amp;", "&");
    }

    public static String formBody(Map<String, String> fields) {
        StringBuilder body = new StringBuilder();
        fields.forEach((k, v) -> body.append(body.length() == 0 ? "" : "&").append(enc(k)).append('=').append(enc(v)));
        return body.toString();
    }

    public static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
