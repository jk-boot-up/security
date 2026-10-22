package com.jk.explore.security.bffspring;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * The single-page app: a public client that signs the customer in with the authorization code flow and PKCE, gets tokens in the browser, and calls the accounts API with them.
 * Given a key, it asks for tokens bound to that key and proves possession of the key with each call.
 */
public final class SpaClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    static final String REDIRECT = "https://app.bank.example/callback";

    /** A token response. */
    public record Tokens(int status, Map<String, Object> json) {

        public String string(String name) {
            return json.get(name) == null ? null : String.valueOf(json.get(name));
        }
    }

    /** An answer from the accounts API. */
    public record ApiReply(int status, String body, String challenge) {
    }

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final String issuer;
    private final String apiUrl;

    public SpaClient(String issuer, String apiUrl) {
        this.issuer = issuer;
        this.apiUrl = apiUrl;
    }

    /** Signs Ana in and exchanges the code, with a DPoP proof at the token endpoint when a key is given. */
    public Tokens signIn(String clientId, DpopKey key) throws IOException, InterruptedException {
        String verifier = verifier();
        String url = issuer + "/protocol/openid-connect/auth?response_type=code&client_id=" + clientId + "&scope=" + Shopper.enc("openid accounts.read payments.write") + "&state=s&redirect_uri="
                + Shopper.enc(REDIRECT) + "&code_challenge=" + challenge(verifier) + "&code_challenge_method=S256";
        Shopper.Page stopped = new Shopper().navigate(url, "ana", "demo-password", REDIRECT);
        String code = Pattern.compile("[?&]code=([^&]+)").matcher(stopped.url()).results().map(m -> m.group(1)).findFirst().orElseThrow();
        return token("grant_type=authorization_code&client_id=" + clientId + "&code=" + Shopper.enc(code) + "&redirect_uri=" + Shopper.enc(REDIRECT) + "&code_verifier=" + verifier, key);
    }

    /** Exchanges a refresh token, the way the app does when the access token has run out, or the way a thief would. */
    public Tokens refresh(String clientId, String refreshToken, DpopKey key) throws IOException, InterruptedException {
        return token("grant_type=refresh_token&client_id=" + clientId + "&refresh_token=" + Shopper.enc(refreshToken), key);
    }

    private Tokens token(String form, DpopKey key) throws IOException, InterruptedException {
        String endpoint = issuer + "/protocol/openid-connect/token";
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(endpoint)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form));
        if (key != null) {
            b.header("DPoP", key.proof("POST", endpoint, null));
        }
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Tokens(r.statusCode(), JSON.readValue(r.body(), new TypeReference<Map<String, Object>>() {
        }));
    }

    /** Calls the API with a plain bearer token. */
    public ApiReply bearer(String method, String path, String accessToken) throws IOException, InterruptedException {
        return call(method, path, "Bearer " + accessToken, null);
    }

    /** Calls the API with a DPoP-bound token and the given proof. */
    public ApiReply dpop(String method, String path, String accessToken, String proof) throws IOException, InterruptedException {
        return call(method, path, "DPoP " + accessToken, proof);
    }

    /** The address a proof for a path must name. */
    public String urlOf(String path) {
        return apiUrl + path;
    }

    private ApiReply call(String method, String path, String authorization, String proof) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(apiUrl + path)).header("Authorization", authorization)
                .method(method, method.equals("GET") ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(""));
        if (proof != null) {
            b.header("DPoP", proof);
        }
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new ApiReply(r.statusCode(), r.body(), r.headers().allValues("WWW-Authenticate").stream().reduce((a, c) -> a + " | " + c).orElse(null));
    }

    public static Map<String, Object> claims(String jwt) {
        return JSON.readValue(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), new TypeReference<Map<String, Object>>() {
        });
    }

    private static String verifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String challenge(String verifier) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
