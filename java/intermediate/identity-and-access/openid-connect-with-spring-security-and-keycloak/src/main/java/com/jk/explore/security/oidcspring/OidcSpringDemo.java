package com.jk.explore.security.oidcspring;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Runs the store's OpenID Connect story in five numbered acts against a real Keycloak in Docker and a real Spring Security 7 client, all on the loopback interface. */
public class OidcSpringDemo {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Ports, codes, tokens and session ids are never printed. */
    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        int port = Running.freePort();
        try (KeycloakLab keycloak = KeycloakLab.start(port); Running store = Running.start(keycloak, port); LocalProvider provider = new LocalProvider(Clock.systemUTC())) {
            Tokens tokens = new Tokens(keycloak.issuer("bank"), store.url());
            act1SignIn(out, keycloak, store, tokens);
            act2CheckTheIdToken(out, store, provider);
            act3DiscoveryAndKeys(out, keycloak, store, tokens, provider);
            act4WhoSignedIn(out, keycloak, store, tokens);
            act5SigningOut(out, keycloak, store, tokens);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** A section header: what follows is one input or one result. */
    private static void section(StringBuilder out, String header) {
        line(out, "  " + header);
    }

    private static void item(StringBuilder out, String text) {
        line(out, "    " + text);
    }

    private static String pad(String text, int width) {
        return String.format("%-" + width + "s", text);
    }

    /** Keycloak's address changes with its port, so it is printed as a placeholder. */
    static String local(String text) {
        return String.valueOf(text).replaceAll("http://127\\.0\\.0\\.1:\\d+", "<keycloak>");
    }

    static Map<String, Object> json(String body) {
        return JSON.readValue(body, new TypeReference<Map<String, Object>>() {
        });
    }

    private static String param(String url, String name) {
        Matcher m = Pattern.compile("[?&]" + name + "=([^&]+)").matcher(url);
        return m.find() ? java.net.URLDecoder.decode(m.group(1), java.nio.charset.StandardCharsets.UTF_8) : null;
    }

    private static void act1SignIn(StringBuilder out, KeycloakLab keycloak, Running store, Tokens tokens) throws Exception {
        line(out, "ONE. OAuth gives access; OpenID Connect adds who signed in.");
        Shopper ana = new Shopper();
        HttpResponse<String> start = ana.once(store.url() + "/oauth2/authorization/bank");
        String authorize = start.headers().firstValue("Location").orElseThrow();
        section(out, "THE REQUEST (the store sends Ana's browser to Keycloak):");
        item(out, local(authorize.substring(0, authorize.indexOf('?'))));
        item(out, pad("response_type", 23) + param(authorize, "response_type"));
        item(out, pad("client_id", 23) + param(authorize, "client_id"));
        item(out, pad("scope", 23) + param(authorize, "scope"));
        item(out, pad("state", 23) + (param(authorize, "state") != null ? "<random, kept in her session>" : "missing"));
        item(out, pad("nonce", 23) + (param(authorize, "nonce") != null ? "<random, kept in her session>" : "missing"));
        item(out, pad("code_challenge", 23) + (param(authorize, "code_challenge") != null ? "<SHA-256 of a random verifier>" : "missing"));
        item(out, pad("code_challenge_method", 23) + param(authorize, "code_challenge_method"));
        Shopper.Page page = ana.navigate(store.url() + "/oauth2/authorization/bank", "ana", "demo-password");
        Map<String, Object> me = json(ana.get(store.url() + "/me").body());
        String nonceSent = param(page.hops().get(1), "nonce");
        section(out, "THE SIGN-IN:");
        item(out, "Ana types her password at Keycloak, not the store: " + page.hops().get(1).startsWith(keycloak.base()));
        item(out, "the store redeems the code, server to server: " + page.hops().stream().anyMatch(h -> h.contains("/login/oauth2/code/bank?")));
        Tokens.Reply reply = tokens.signIn("bank", "ana", "openid email");
        Map<String, Object> idClaims = Tokens.claims(reply.string("id_token"));
        Map<String, Object> accessClaims = Tokens.claims(reply.string("access_token"));
        @SuppressWarnings("unchecked")
        List<String> names = new java.util.ArrayList<>((java.util.Collection<String>) me.get("claims"));
        List<String> shown = List.of("iss", "sub", "aud", "azp", "typ", "nonce", "email", "email_verified");
        section(out, "THE ID TOKEN'S CLAIMS (" + names.size() + " claims, as the store sees them):");
        item(out, pad("iss", 7) + local(String.valueOf(me.get("issuer"))));
        item(out, pad("sub", 7) + me.get("subject"));
        item(out, pad("aud", 7) + idClaims.get("aud") + ", azp " + idClaims.get("azp") + ", typ " + idClaims.get("typ"));
        item(out, pad("nonce", 7) + "<random>, the one sent in this sign-in: " + (nonceSent != null && nonceSent.equals(me.get("nonce"))));
        item(out, pad("email", 7) + me.get("email") + ", email_verified " + me.get("emailVerified"));
        names.removeAll(shown);
        item(out, pad("also", 7) + String.join(", ", names.subList(0, Math.min(6, names.size()))) + ",");
        item(out, pad("", 7) + String.join(", ", names.subList(Math.min(6, names.size()), names.size())));
        section(out, "THE RESULT:");
        item(out, "Ana is signed in to the store as");
        item(out, local(String.valueOf(me.get("issuer"))) + " | " + me.get("subject"));
        section(out, "THE TWO TOKENS IN THE REPLY (Keycloak's token endpoint):");
        item(out, pad("ID token", 14) + pad("typ " + idClaims.get("typ"), 12) + "aud " + idClaims.get("aud") + ": for the store");
        item(out, pad("access token", 14) + pad("typ " + accessClaims.get("typ"), 12) + (accessClaims.containsKey("aud") ? "aud " + accessClaims.get("aud") : "no aud claim")
                + ": for the API");
        JwtDecoder decoder = new OidcIdTokenDecoderFactory().createDecoder(store.bean(ClientRegistrationRepository.class).findByRegistrationId("bank"));
        section(out, "THE ACCESS TOKEN, OFFERED AS PROOF OF SIGN-IN:");
        item(out, refusal(() -> decoder.decode(reply.string("access_token"))));
        item(out, "<- AN ACCESS TOKEN SAYS WHAT IS ALLOWED, NOT WHO");
        Tokens.Reply noOpenid = tokens.signIn("bank", "ana", "email");
        section(out, "A SIGN-IN WITHOUT THE openid SCOPE (scope email):");
        item(out, "no ID token in the reply: " + (noOpenid.string("id_token") == null) + "; only API access");
        section(out, "A CALLBACK FROM ANOTHER SIGN-IN, IN ANA'S BROWSER:");
        item(out, wrongState(store));
    }

    /** Mallory signs in and her callback address, with her own state, is opened in Ana's browser, which started its own sign-in. */
    private static String wrongState(Running store) throws IOException, InterruptedException {
        Shopper mallory = new Shopper();
        Shopper.Page callback = mallory.navigate(store.url() + "/oauth2/authorization/bank", "ana", "demo-password", store.url() + "/login/oauth2/code/bank");
        Shopper anaBrowser = new Shopper();
        anaBrowser.once(store.url() + "/oauth2/authorization/bank");
        Shopper.Page result = anaBrowser.get(callback.url());
        return result.url().contains("error") || result.status() >= 400 ? "refused: no sign-in in this browser has that state" : "accepted";
    }

    /** Spring Security's own message, with its long fixed prefixes shortened so that a line fits on a slide. */
    private static String refusal(java.util.concurrent.Callable<Object> attempt) {
        try {
            attempt.call();
            return "accepted";
        } catch (Exception e) {
            String text = String.valueOf(e.getMessage()).replace("An error occurred while attempting to decode the Jwt: ", "")
                    .replace("The ID Token contains invalid claims: ", "invalid claims ").replaceAll("\\s+", " ");
            if (text.startsWith("Jwt expired at")) {
                text = "expired";
            }
            return "refused: " + text;
        }
    }

    private static JwtDecoder labDecoder(LocalProvider provider) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("lab").clientId(LocalProvider.CLIENT).clientSecret("unused")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri("http://127.0.0.1/cb").authorizationUri("https://bank.example/auth")
                .tokenUri("https://bank.example/token").jwkSetUri(provider.jwksUri()).issuerUri(LocalProvider.ISSUER).scope("openid").build();
        OidcIdTokenDecoderFactory factory = new OidcIdTokenDecoderFactory();
        factory.setJwtValidatorFactory(r -> {
            OidcIdTokenValidator oidc = new OidcIdTokenValidator(r);
            oidc.setClockSkew(SKEW);
            return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(SKEW), oidc);
        });
        return factory.createDecoder(registration);
    }

    static final Duration SKEW = Duration.ofSeconds(30);

    private static void act2CheckTheIdToken(StringBuilder out, Running store, LocalProvider p) throws JOSEException {
        line(out, "TWO. Checking the ID token, as OpenID Connect Core requires.");
        JwtDecoder decoder = labDecoder(p);
        section(out, "THE STORE'S SETTINGS (every ID token is checked against them):");
        item(out, pad("issuer", 12) + LocalProvider.ISSUER);
        item(out, pad("client_id", 12) + LocalProvider.CLIENT);
        item(out, pad("keys", 12) + "the provider's jwks.json, key " + p.keyId());
        item(out, pad("clock skew", 12) + SKEW.toSeconds() + " seconds");
        JWTClaimsSet good = p.claims().build();
        String token = p.sign(good);
        String[] parts = token.split("\\.");
        section(out, "BEFORE SIGNING (a test provider makes the ID token):");
        item(out, "header  " + new String(java.util.Base64.getUrlDecoder().decode(parts[0]), java.nio.charset.StandardCharsets.UTF_8));
        item(out, "claims  " + String.join(", ", new java.util.TreeSet<>(good.getClaims().keySet()))
                + "; exp = iat + " + (good.getExpirationTime().getTime() - good.getIssueTime().getTime()) / 1000 + " s");
        section(out, "HOW IT IS SIGNED (step by step):");
        item(out, "1. header part = base64url(header)");
        item(out, "   " + parts[0] + " (" + parts[0].length() + " chars)");
        item(out, "2. claims part = base64url(claims): " + parts[1].length() + " chars");
        item(out, "3. signing input = header part + \".\" + claims part");
        item(out, "4. signature = RSA(private key, SHA-256(signing input))");
        item(out, "   a " + p.keySize() + "-bit key gives " + java.util.Base64.getUrlDecoder().decode(parts[2]).length + " bytes = "
                + parts[2].length() + " base64url chars");
        section(out, "HOW SPRING SECURITY CHECKS IT (in this order):");
        item(out, "1. the signature, with the published key named by kid");
        item(out, "2. exp and iat against now, with " + SKEW.toSeconds() + " seconds of skew");
        item(out, "3. iss exactly, sub present, aud has store-web, azp");
        section(out, "THE RESULT:");
        item(out, verdict(decoder, token));

        section(out, "ONE THING CHANGED AT A TIME (the input, then the result):");
        item(out, "signed with a key the bank never published, kid lab-key");
        item(out, "  -> " + verdict(decoder, p.signWithAnotherKey(good)));
        item(out, "iss https://bank.example/ (one extra slash)");
        item(out, "  -> " + verdict(decoder, p.sign(p.claims().issuer("https://bank.example/").build())));
        item(out, "iss https://wallet.example, signed by the bank");
        item(out, "  -> " + verdict(decoder, p.sign(p.claims().issuer("https://wallet.example").build())));
        item(out, "aud budget-app");
        item(out, "  -> " + verdict(decoder, p.sign(p.claims().audience("budget-app").build())));
        JWTClaimsSet.Builder two = p.claims().audience(List.of("store-web", "budget-app"));
        item(out, "aud [store-web, budget-app], no azp");
        item(out, "  -> " + verdict(decoder, p.sign(two.build())));
        item(out, "aud [store-web, budget-app], azp store-web");
        item(out, "  -> " + verdict(decoder, p.sign(two.claim("azp", "store-web").build())));
        section(out, "THE SUBJECT, THE HEADER AND THE NONCE (input, then result):");
        item(out, "no sub claim");
        item(out, "  -> " + verdict(decoder, p.sign(p.claims().subject(null).build())));
        item(out, "header {\"alg\":\"none\"}, no signature");
        item(out, "  -> " + verdict(decoder, p.unsigned(good)));
        item(out, "the nonce: checked by the sign-in itself, with the state,");
        item(out, "  against the one kept in the browser's session (act one)");
        section(out, "THE TIME CHECK (skew " + SKEW.toSeconds() + " seconds):");
        for (long late : new long[] {29, 31}) {
            item(out, "exp = now - " + late + " s: now " + (late < SKEW.toSeconds() ? "<" : ">") + " exp + " + SKEW.toSeconds());
            item(out, "  -> " + verdict(decoder, p.sign(p.expiredSecondsAgo(late).build())));
        }
    }

    private static String verdict(JwtDecoder decoder, String token) {
        return refusal(() -> decoder.decode(token));
    }

    private static void act3DiscoveryAndKeys(StringBuilder out, KeycloakLab keycloak, Running store, Tokens tokens, LocalProvider p) throws Exception {
        line(out, "THREE. Discovery and keys.");
        String issuer = keycloak.issuer("bank");
        section(out, "THE CONFIGURATION (the store knows one value):");
        item(out, "issuer-uri " + local(issuer));
        section(out, "THE REQUEST:");
        item(out, "GET " + local(issuer) + "/.well-known/");
        item(out, "    openid-configuration");
        ClientRegistration found = ClientRegistrations.fromIssuerLocation(issuer).clientId("store-web").build();
        Map<String, Object> metadata = found.getProviderDetails().getConfigurationMetadata();
        section(out, "THE RESPONSE (the fields the store uses; <iss> = issuer):");
        item(out, pad("issuer", 23) + local(String.valueOf(metadata.get("issuer"))));
        for (String name : List.of("authorization_endpoint", "token_endpoint", "jwks_uri", "end_session_endpoint")) {
            item(out, pad(name, 23) + String.valueOf(metadata.get(name)).replace(issuer, "<iss>"));
        }
        section(out, "THE CHECKS:");
        item(out, "issuer equals the configured issuer: " + issuer.equals(found.getProviderDetails().getIssuerUri()));
        item(out, "the addresses start with the issuer's own address: "
                + (found.getProviderDetails().getAuthorizationUri().startsWith(keycloak.base()) && found.getProviderDetails().getTokenUri().startsWith(keycloak.base())
                        && found.getProviderDetails().getJwkSetUri().startsWith(keycloak.base())));
        section(out, "FORGED METADATA (a test provider; input, then result):");
        p.serveDiscovery("https://wallet.example", p.jwksUri());
        item(out, "issuer https://wallet.example");
        item(out, "  -> " + refusalOf(() -> ClientRegistrations.fromIssuerLocation(p.issuerAddress()).clientId("x").build()));
        item(out, "  <- ONE PROVIDER CANNOT POSE AS ANOTHER");
        p.serveDiscovery(p.issuerAddress(), "https://keys.example/jwks.json");
        ClientRegistration elsewhere = ClientRegistrations.fromIssuerLocation(p.issuerAddress()).clientId("x").build();
        item(out, "jwks_uri https://keys.example/jwks.json");
        item(out, "  -> accepted by Spring Security: " + "https://keys.example/jwks.json".equals(elsewhere.getProviderDetails().getJwkSetUri()));
        item(out, "  <- IT CHECKS THE ISSUER, NOT WHERE THE KEYS ARE,");
        item(out, "  <- SO THE ISSUER ADDRESS MUST BE https AND TRUSTED");
        int before = LocalProvider.kidsOf(keycloak.certs("bank")).size();
        JwtDecoder decoder = new OidcIdTokenDecoderFactory().createDecoder(store.bean(ClientRegistrationRepository.class).findByRegistrationId("bank"));
        String oldToken = tokens.signIn("bank", "ana", "openid email").string("id_token");
        section(out, "KEY ROTATION (step by step, on the real Keycloak):");
        item(out, "1. Keycloak publishes " + before + " signing key");
        item(out, "   a token signed with it: " + refusal(() -> decoder.decode(oldToken)));
        keycloak.addSigningKey("bank");
        int after = LocalProvider.kidsOf(keycloak.certs("bank")).size();
        String newToken = tokens.signIn("bank", "ana", "openid email").string("id_token");
        item(out, "2. the bank adds a key and signs new tokens with it");
        item(out, "   keys published: " + after + "; the new token's kid is new: "
                + !Tokens.header(newToken).get("kid").equals(Tokens.header(oldToken).get("kid")));
        item(out, "3. the store reloads the key set on its own");
        item(out, "   old key's token: " + refusal(() -> decoder.decode(oldToken)) + "; new key's: " + refusal(() -> decoder.decode(newToken)));
    }

    private static String refusalOf(java.util.concurrent.Callable<Object> attempt) {
        try {
            attempt.call();
            return "accepted";
        } catch (Exception e) {
            return e.getMessage() != null && e.getMessage().startsWith("The Issuer") ? "refused: the metadata names another issuer" : "refused: " + e.getMessage();
        }
    }

    private static void act4WhoSignedIn(StringBuilder out, KeycloakLab keycloak, Running store, Tokens tokens) throws Exception {
        line(out, "FOUR. Who signed in: issuer and subject, never the email.");
        Shopper ana = new Shopper();
        ana.navigate(store.url() + "/oauth2/authorization/bank", "ana", "demo-password");
        Shopper mallory = new Shopper();
        mallory.navigate(store.url() + "/oauth2/authorization/wallet", "mal", "demo-password");
        Map<String, Object> anaMe = json(ana.get(store.url() + "/me").body());
        Map<String, Object> malMe = json(mallory.get(store.url() + "/me").body());
        section(out, "THE TWO PROVIDERS SAY (realm, subject, e-mail address):");
        item(out, pad("bank", 8) + anaMe.get("subject"));
        item(out, pad("", 8) + anaMe.get("email") + ", verified " + anaMe.get("emailVerified"));
        item(out, pad("wallet", 8) + malMe.get("subject"));
        item(out, pad("", 8) + malMe.get("email") + ", verified " + malMe.get("emailVerified"));
        section(out, "BEFORE THE FIX (the store keys accounts by e-mail address):");
        item(out, "Mallory signs in with the wallet and gets:");
        item(out, "  " + json(mallory.get(store.url() + "/account?keyedBy=email").body()).get("account") + "  <- ACCOUNT TAKEOVER");
        section(out, "AFTER THE FIX (accounts keyed by issuer and subject):");
        item(out, "Ana gets: " + json(ana.get(store.url() + "/account?keyedBy=subject").body()).get("account"));
        item(out, "Mallory gets:");
        item(out, "  " + json(mallory.get(store.url() + "/account?keyedBy=subject").body()).get("account"));
        Tokens.Reply reply = tokens.signIn("bank", "ana", "openid email");
        HttpResponse<String> info = tokens.userinfo(reply.string("access_token"));
        section(out, "THE USERINFO REQUEST (input, then result):");
        item(out, "Ana's access token");
        item(out, "  -> " + info.statusCode() + ", sub " + json(info.body()).get("sub"));
        item(out, "  -> the same subject as the ID token: " + json(info.body()).get("sub").equals(Tokens.claims(reply.string("id_token")).get("sub")));
        item(out, "the ID token instead");
        item(out, "  -> " + (tokens.userinfo(reply.string("id_token")).statusCode() == 401 ? "refused (401)" : "accepted"));
    }

    private static void act5SigningOut(StringBuilder out, KeycloakLab keycloak, Running store, Tokens tokens) throws Exception {
        line(out, "FIVE. Signing out, and the bill.");
        Shopper ana = new Shopper();
        ana.navigate(store.url() + "/oauth2/authorization/bank", "ana", "demo-password");
        String csrf = String.valueOf(json(ana.get(store.url() + "/csrf").body()).get("token"));
        Shopper.Page logout = ana.post(store.url() + "/logout", "_csrf=" + Shopper.enc(csrf));
        section(out, "ANA SIGNS OUT AT THE STORE (RP-initiated logout):");
        item(out, "the store's session ended: " + (ana.get(store.url() + "/me").status() == 401));
        item(out, "her browser is sent to");
        item(out, local(logout.url().substring(0, logout.url().indexOf('?'))));
        item(out, "with id_token_hint and post_logout_redirect_uri: "
                + (logout.url().contains("/protocol/openid-connect/logout") && logout.url().contains("id_token_hint=") && logout.url().contains("post_logout_redirect_uri=")));
        Shopper.Page back = ana.navigate(logout.url(), null, null);
        item(out, "Keycloak ends its session, sends her back: " + back.url().startsWith(store.url() + "/"));
        Shopper.Page again = ana.get(store.url() + "/oauth2/authorization/bank");
        item(out, "signing in again asks for her password: " + again.body().contains("kc-form-login"));

        Shopper laptop = new Shopper();
        Shopper phone = new Shopper();
        laptop.navigate(store.url() + "/oauth2/authorization/bank", "ana", "demo-password");
        phone.navigate(store.url() + "/oauth2/authorization/bank", "ana", "demo-password");
        String laptopSession = String.valueOf(json(laptop.get(store.url() + "/me").body()).get("sid"));
        String phoneSession = String.valueOf(json(phone.get(store.url() + "/me").body()).get("sid"));
        section(out, "BEFORE THE LOGOUT TOKEN (Ana on a laptop and a phone):");
        item(out, "two Keycloak sessions, two sids: " + !laptopSession.equals(phoneSession));
        int ended = keycloak.endSession("bank", laptopSession);
        boolean laptopOut = false;
        for (int i = 0; i < 40 && !laptopOut; i++) {
            laptopOut = laptop.get(store.url() + "/me").status() == 401;
            if (!laptopOut) {
                Thread.sleep(250);
            }
        }
        section(out, "THE LOGOUT TOKEN (Keycloak, server to server):");
        item(out, "Keycloak ends the laptop's session (answer " + ended + ")");
        item(out, "and POSTs a logout token with that sid to the store:");
        item(out, "/logout/connect/back-channel/bank");
        section(out, "AFTER THE LOGOUT TOKEN:");
        item(out, "laptop signed in: " + !laptopOut + "; phone signed in: " + (phone.get(store.url() + "/me").status() == 200));
        Tokens.Reply reply = tokens.signIn("bank", "ana", "openid email");
        java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
        section(out, "BROKEN LOGOUT TOKENS (input, then result):");
        for (String[] bad : List.of(new String[] {"a made-up logout token, not.a.token", "not.a.token"}, new String[] {"an ID token sent as a logout token", reply.string("id_token")})) {
            int status = http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(store.url() + "/logout/connect/back-channel/bank")).header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString("logout_token=" + Shopper.enc(bad[1]))).build(), java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
            item(out, bad[0]);
            item(out, "  -> refused, the store answered " + status + "; phone still in: " + (phone.get(store.url() + "/me").status() == 200));
        }
        section(out, "THE BILL:");
        item(out, "many checks and options: use a certified library");
        item(out, "the ID token stays in the store; never take an access token");
        item(out, "  as a sign-in");
        item(out, "sign-out must reach the store, the provider and every app");
    }
}
