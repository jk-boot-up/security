package com.jk.explore.security.passkeysspring;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * A customer's browser and the bank's page script as far as this lesson needs them: it keeps the session cookie, sends the CSRF token in a header, and does what
 * {@code navigator.credentials.create} and {@code navigator.credentials.get} would do, by handing the bank's options to an authenticator and posting what comes back.
 */
final class PasskeyBrowser {

    static final JsonMapper JSON = JsonMapper.builder().build();

    record Reply(int status, String body) {

        Map<String, Object> json() {
            return JSON.readValue(body, new TypeReference<Map<String, Object>>() {
            });
        }
    }

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NEVER).build();
    private final String base;
    private final Map<String, String> cookies = new LinkedHashMap<>();

    PasskeyBrowser(String base) {
        this.base = base;
    }

    Reply signInWithPassword(String user, String password) throws IOException, InterruptedException {
        return send("POST", "/login", "application/x-www-form-urlencoded", "username=" + enc(user) + "&password=" + enc(password) + "&_csrf=" + enc(csrf()));
    }

    Reply me() throws IOException, InterruptedException {
        return send("GET", "/me", null, null);
    }

    /** What the page and the browser did with a request for a passkey: either the browser or the authenticator found nothing to use, or the bank answered. */
    record Attempt(String nothing, Reply reply) {

        boolean blocked() {
            return nothing != null;
        }

        int status() {
            return reply == null ? 0 : reply.status();
        }

        /** For the demo's output. */
        String text(String signedInAs) {
            return blocked() ? "nothing (" + nothing + ")" : reply.status() == 200 ? "accepted (" + signedInAs + ")" : "refused (" + reply.status() + ")";
        }
    }

    /** What the bank's page does when a signed-in customer adds a passkey. */
    Reply registerPasskey(SoftwareAuthenticator authenticator, String label, boolean userVerified, String originOfPage, String rpId) throws IOException, InterruptedException {
        Map<String, Object> options = registrationOptions();
        String challenge = String.valueOf(options.get("challenge"));
        @SuppressWarnings("unchecked")
        String userHandle = String.valueOf(((Map<String, Object>) options.get("user")).get("id"));
        SoftwareAuthenticator.Registration r = authenticator.create(rpId, originOfPage, challenge, userVerified, userHandle);
        String body = "{\"publicKey\":{\"credential\":{\"id\":\"" + r.credentialId() + "\",\"rawId\":\"" + r.credentialId() + "\",\"response\":{\"attestationObject\":\"" + r.attestationObject()
                + "\",\"clientDataJSON\":\"" + r.clientDataJson() + "\",\"transports\":[\"internal\"]},\"type\":\"public-key\",\"clientExtensionResults\":{},\"authenticatorAttachment\":\"platform\"},\"label\":\""
                + label + "\"}}";
        return send("POST", "/webauthn/register", "application/json", body);
    }

    Map<String, Object> registrationOptions() throws IOException, InterruptedException {
        return send("POST", "/webauthn/register/options", "application/json", "{}").json();
    }

    Map<String, Object> authenticationOptions() throws IOException, InterruptedException {
        return send("POST", "/webauthn/authenticate/options", "application/json", "{}").json();
    }

    /**
     * What the page does at sign-in. The browser first applies its own rule: a page may only ask for its own host or a parent domain of it as the relying party id. The
     * authenticator then looks for a passkey made for exactly that id. Only then is the bank asked for a challenge, and the authenticator's answer posted.
     */
    Attempt signInWithPasskey(SoftwareAuthenticator authenticator, boolean userVerified, String originOfPage, String rpIdAsked) throws IOException, InterruptedException {
        String host = URI.create(originOfPage).getHost();
        if (!host.equals(rpIdAsked) && !host.endsWith("." + rpIdAsked)) {
            return new Attempt("the browser will not use " + rpIdAsked + " for a page at " + originOfPage, null);
        }
        if (!authenticator.hasPasskeyFor(rpIdAsked)) {
            return new Attempt("the authenticator has no passkey for " + rpIdAsked, null);
        }
        String challenge = String.valueOf(authenticationOptions().get("challenge"));
        return new Attempt(null, postAssertion(authenticator.sign(rpIdAsked, originOfPage, challenge, userVerified, true)));
    }

    /** The same, by a browser that skipped the rules, so that only the bank's own checks stand in the way. */
    Reply signInWithPasskeyWithoutBrowserRules(SoftwareAuthenticator authenticator, boolean userVerified, String originOfPage, String rpIdSigned) throws IOException, InterruptedException {
        String challenge = String.valueOf(authenticationOptions().get("challenge"));
        return postAssertion(authenticator.sign(rpIdSigned, originOfPage, challenge, userVerified, true));
    }

    Reply postAssertion(SoftwareAuthenticator.Assertion a) throws IOException, InterruptedException {
        String body = "{\"id\":\"" + a.credentialId() + "\",\"rawId\":\"" + a.credentialId() + "\",\"response\":{\"authenticatorData\":\"" + a.authenticatorData() + "\",\"clientDataJSON\":\""
                + a.clientDataJson() + "\",\"signature\":\"" + a.signature() + "\",\"userHandle\":\"" + a.userHandle() + "\"},\"type\":\"public-key\",\"clientExtensionResults\":{},\"authenticatorAttachment\":\"platform\"}";
        return send("POST", "/login/webauthn", "application/json", body);
    }

    String csrf() throws IOException, InterruptedException {
        return String.valueOf(send("GET", "/csrf", null, null).json().get("token"));
    }

    Reply send(String method, String path, String contentType, String body) throws IOException, InterruptedException {
        return send(method, path, contentType, body, true);
    }

    /** One request; {@code withCsrf} says whether the page's script adds the CSRF token header, as it does to every post. */
    Reply send(String method, String path, String contentType, String body, boolean withCsrf) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path)).method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (contentType != null) {
            b.header("Content-Type", contentType);
        }
        if (withCsrf && !path.equals("/csrf") && !path.equals("/login") && !method.equals("GET")) {
            b.header("X-CSRF-TOKEN", csrf());
        }
        if (!cookies.isEmpty()) {
            StringBuilder cookie = new StringBuilder();
            cookies.forEach((k, v) -> cookie.append(cookie.length() == 0 ? "" : "; ").append(k).append('=').append(v));
            b.header("Cookie", cookie.toString());
        }
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        List<String> set = r.headers().allValues("Set-Cookie");
        for (String header : set) {
            String pair = header.split(";", 2)[0];
            int eq = pair.indexOf('=');
            cookies.put(pair.substring(0, eq), pair.substring(eq + 1));
        }
        return new Reply(r.statusCode(), r.body());
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
