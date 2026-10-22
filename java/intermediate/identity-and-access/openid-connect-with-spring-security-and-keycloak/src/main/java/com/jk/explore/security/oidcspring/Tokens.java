package com.jk.explore.security.oidcspring;

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
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gets the tokens that the store itself keeps out of the browser, so that the demo can look at them: a sign-in as the store would start it, stopped at the callback
 * address, and the code redeemed at the token endpoint the way the store redeems it.
 */
final class Tokens {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    record Reply(int status, Map<String, Object> json) {

        String string(String name) {
            return json.get(name) == null ? null : String.valueOf(json.get(name));
        }
    }

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private final String issuer;
    private final String storeUrl;

    Tokens(String issuer, String storeUrl) {
        this.issuer = issuer;
        this.storeUrl = storeUrl;
    }

    /** Signs a user in at the provider with the given scope and redeems the code the way the store would. */
    Reply signIn(String registrationId, String user, String scope) throws IOException, InterruptedException {
        String verifier = verifier();
        String redirect = storeUrl + "/login/oauth2/code/" + registrationId;
        String url = issuer + "/protocol/openid-connect/auth?response_type=code&client_id=store-web&scope=" + Shopper.enc(scope) + "&state=lab&nonce=lab-nonce"
                + "&redirect_uri=" + Shopper.enc(redirect) + "&code_challenge=" + challenge(verifier) + "&code_challenge_method=S256";
        Shopper.Page stopped = new Shopper().navigate(url, user, "demo-password", redirect);
        String code = java.util.regex.Pattern.compile("[?&]code=([^&]+)").matcher(stopped.url()).results().map(m -> m.group(1)).findFirst().orElseThrow();
        return redeem(code, redirect, verifier);
    }

    Reply redeem(String code, String redirect, String verifier) throws IOException, InterruptedException {
        String form = "grant_type=authorization_code&client_id=store-web&client_secret=demo-store-secret&code=" + Shopper.enc(code) + "&redirect_uri=" + Shopper.enc(redirect)
                + (verifier == null ? "" : "&code_verifier=" + verifier);
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(issuer + "/protocol/openid-connect/token")).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(r.statusCode(), JSON.readValue(r.body(), new TypeReference<Map<String, Object>>() {
        }));
    }

    /** What the provider's userinfo endpoint says to a bearer token. */
    HttpResponse<String> userinfo(String token) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(URI.create(issuer + "/protocol/openid-connect/userinfo")).header("Authorization", "Bearer " + token).build(), HttpResponse.BodyHandlers.ofString());
    }

    static Map<String, Object> claims(String jwt) {
        return JSON.readValue(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]), new TypeReference<Map<String, Object>>() {
        });
    }

    static Map<String, Object> header(String jwt) {
        return JSON.readValue(Base64.getUrlDecoder().decode(jwt.split("\\.")[0]), new TypeReference<Map<String, Object>>() {
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
