package com.jk.explore.security.oauthkeycloak;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * The budgeting app and the customer's browser, as far as this lesson needs them. The app builds the authorization request, remembers the state and the verifier, and calls the
 * token endpoint directly. The browser follows the one redirect that matters: it shows Keycloak's pages to Ana, takes her password and her consent to Keycloak and
 * never to the app, and hands back the address Keycloak redirects to. It keeps Keycloak's cookies itself, because Keycloak marks them Secure and a browser sends them to
 * 127.0.0.1 over http while Java's cookie manager does not.
 */
final class OAuthClient {

    static final String REDIRECT = "https://budget.example/callback";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** What came back from the authorization endpoint: a redirect address, or an error page Keycloak refused to redirect from. */
    record Outcome(int status, String location, String errorPage, boolean consentShown) {

        boolean redirected() {
            return location != null;
        }

        /** The query (or fragment) parameters of the redirect address. */
        Map<String, String> parameters() {
            Map<String, String> out = new LinkedHashMap<>();
            if (location == null) {
                return out;
            }
            String text = location.contains("?") ? location.substring(location.indexOf('?') + 1) : location.contains("#") ? location.substring(location.indexOf('#') + 1) : "";
            for (String pair : text.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    out.put(pair.substring(0, eq), java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
                }
            }
            return out;
        }

        /** The error and its description, as {@code error: description}. */
        String error() {
            Map<String, String> p = parameters();
            return p.get("error") == null ? null : p.get("error") + ": " + p.get("error_description");
        }
    }

    record TokenReply(int status, Map<String, Object> json) {

        boolean ok() {
            return status == 200;
        }

        String string(String name) {
            return json.get(name) == null ? null : String.valueOf(json.get(name));
        }

        /** The error and its description, as {@code error: description}. */
        String error() {
            return string("error") == null ? null : string("error") + ": " + string("error_description");
        }
    }

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NEVER).build();
    private final String base;
    private final String issuer;

    OAuthClient(String base, String issuer) {
        this.base = base;
        this.issuer = issuer;
    }

    static String newVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String challenge(String verifier) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String newState() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** The authorization request of the budgeting app, with the state and the PKCE challenge, and the given overrides. */
    static Map<String, String> authorizationRequest(String clientId, String redirect, String scope, String state, String challenge) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("response_type", "code");
        p.put("client_id", clientId);
        p.put("redirect_uri", redirect);
        p.put("scope", scope);
        p.put("state", state);
        if (challenge != null) {
            p.put("code_challenge", challenge);
            p.put("code_challenge_method", "S256");
        }
        return p;
    }

    /** Ana's browser goes to Keycloak with this request, signs in, agrees if asked, and comes back with the redirect address. */
    Outcome signIn(Map<String, String> request, String user, String password) throws IOException, InterruptedException {
        Map<String, String> jar = new LinkedHashMap<>();
        HttpResponse<String> page = get(issuer + "/protocol/openid-connect/auth?" + query(request), jar);
        if (page.statusCode() == 302) {
            return new Outcome(302, page.headers().firstValue("Location").orElseThrow(), null, false);
        }
        Matcher form = Pattern.compile("id=\"kc-form-login\"[^>]*action=\"([^\"]+)\"").matcher(page.body());
        if (!form.find()) {
            return new Outcome(page.statusCode(), null, errorText(page.body()), false);
        }
        HttpResponse<String> submitted = post(form.group(1).replace("&amp;", "&"),
                "username=" + enc(user) + "&password=" + enc(password) + "&credentialId=", jar);
        if (submitted.statusCode() != 302) {
            return new Outcome(submitted.statusCode(), null, "the sign-in page refused the password", false);
        }
        String location = submitted.headers().firstValue("Location").orElseThrow();
        boolean consent = false;
        if (location.contains("required-action") && location.contains("OAUTH_GRANT")) {
            HttpResponse<String> consentPage = get(location, jar);
            Matcher action = Pattern.compile("action=\"([^\"]*login-actions/consent[^\"]*)\"").matcher(consentPage.body());
            Matcher code = Pattern.compile("name=\"code\" value=\"([^\"]+)\"").matcher(consentPage.body());
            if (!action.find() || !code.find()) {
                return new Outcome(consentPage.statusCode(), null, "no consent form", false);
            }
            consent = consentPage.body().contains("Read your accounts");
            HttpResponse<String> granted = post(base + action.group(1).replace("&amp;", "&"), "code=" + enc(code.group(1)) + "&accept=Yes", jar);
            location = granted.headers().firstValue("Location").orElseThrow(() -> new IllegalStateException("no redirect after consent: " + granted.statusCode()));
        }
        return new Outcome(302, location, null, consent);
    }

    /** The token endpoint, called by the app directly and never through the browser. */
    TokenReply token(Map<String, String> form) throws IOException, InterruptedException {
        StringBuilder body = new StringBuilder();
        form.forEach((k, v) -> body.append(body.length() == 0 ? "" : "&").append(enc(k)).append('=').append(enc(v)));
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(issuer + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), HttpResponse.BodyHandlers.ofString());
        return new TokenReply(r.statusCode(), JSON.readValue(r.body(), new TypeReference<Map<String, Object>>() {
        }));
    }

    /** Keycloak's own check of an access token, which asks the session: 200 while the session lives, 401 after it ends. */
    int userinfo(String accessToken) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(URI.create(issuer + "/protocol/openid-connect/userinfo")).header("Authorization", "Bearer " + accessToken).build(),
                HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    /** The authorization code in a redirect, after the app has checked that the state is the one it sent. */
    static String codeIfStateMatches(Outcome outcome, String expectedState) {
        Map<String, String> p = outcome.parameters();
        if (!expectedState.equals(p.get("state"))) {
            throw new IllegalStateException("invalid_state: this browser did not start this sign-in");
        }
        return p.get("code");
    }

    private HttpResponse<String> get(String url, Map<String, String> jar) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url));
        cookies(b, jar);
        return remember(jar, http.send(b.build(), HttpResponse.BodyHandlers.ofString()));
    }

    private HttpResponse<String> post(String url, String form, Map<String, String> jar) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form));
        cookies(b, jar);
        return remember(jar, http.send(b.build(), HttpResponse.BodyHandlers.ofString()));
    }

    private static void cookies(HttpRequest.Builder b, Map<String, String> jar) {
        if (!jar.isEmpty()) {
            StringBuilder text = new StringBuilder();
            jar.forEach((k, v) -> text.append(text.length() == 0 ? "" : "; ").append(k).append('=').append(v));
            b.header("Cookie", text.toString());
        }
    }

    private static HttpResponse<String> remember(Map<String, String> jar, HttpResponse<String> response) {
        for (String header : response.headers().allValues("Set-Cookie")) {
            String pair = header.split(";", 2)[0];
            int eq = pair.indexOf('=');
            jar.put(pair.substring(0, eq), pair.substring(eq + 1));
        }
        return response;
    }

    private static String errorText(String html) {
        Matcher m = Pattern.compile("id=\"kc-error-message\".*?<p[^>]*>(.*?)</p>", Pattern.DOTALL).matcher(html);
        return m.find() ? m.group(1).strip() : "an error page";
    }

    private static String query(Map<String, String> p) {
        StringBuilder text = new StringBuilder();
        p.forEach((k, v) -> text.append(text.length() == 0 ? "" : "&").append(enc(k)).append('=').append(enc(v)));
        return text.toString();
    }

    static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
