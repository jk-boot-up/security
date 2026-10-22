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
        line(out, "  the store sends Ana to the bank with scope \"" + param(authorize, "scope") + "\", and a state, a nonce and a PKCE challenge kept for her browser: "
                + (param(authorize, "state") != null && param(authorize, "nonce") != null && "S256".equals(param(authorize, "code_challenge_method"))));
        Shopper.Page page = ana.navigate(store.url() + "/oauth2/authorization/bank", "ana", "demo-password");
        Map<String, Object> me = json(ana.get(store.url() + "/me").body());
        String nonceSent = param(page.hops().get(1), "nonce");
        line(out, "  Ana signs in at Keycloak, never at the store; the store redeems the code itself and starts her session: " + page.hops().stream().anyMatch(h -> h.contains("/login/oauth2/code/bank?")));
        line(out, "  the ID token's claims: " + me.get("claims"));
        line(out, "  Ana is signed in to the store as " + String.valueOf(me.get("issuer")).replaceAll("^http://127.0.0.1:\\d+", "<keycloak>") + " | " + me.get("subject")
                + ", with the e-mail address " + me.get("email") + " (verified: " + me.get("emailVerified") + ")");
        line(out, "  the nonce in her ID token is the one the store sent in this sign-in: " + (nonceSent != null && nonceSent.equals(me.get("nonce"))));
        Tokens.Reply reply = tokens.signIn("bank", "ana", "openid email");
        Map<String, Object> idClaims = Tokens.claims(reply.string("id_token"));
        Map<String, Object> accessClaims = Tokens.claims(reply.string("access_token"));
        line(out, "  the reply holds two tokens: an ID token for the store (typ " + idClaims.get("typ") + ", audience " + idClaims.get("aud") + ") and an access token for the bank's API (typ "
                + accessClaims.get("typ") + ", audience claim: " + accessClaims.containsKey("aud") + ")");
        JwtDecoder decoder = new OidcIdTokenDecoderFactory().createDecoder(store.bean(ClientRegistrationRepository.class).findByRegistrationId("bank"));
        line(out, "  the access token handed to the store as proof of sign-in: " + refusal(() -> decoder.decode(reply.string("access_token"))) + "  <- AN ACCESS TOKEN SAYS WHAT IS ALLOWED, NOT WHO SIGNED IN");
        Tokens.Reply noOpenid = tokens.signIn("bank", "ana", "email");
        line(out, "  a sign-in that asks without the openid scope: no ID token in the reply: " + (noOpenid.string("id_token") == null) + ", only API access");
        line(out, "  a callback from another sign-in, carried to Ana's browser: " + wrongState(store));
    }

    /** Mallory signs in and her callback address, with her own state, is opened in Ana's browser, which started its own sign-in. */
    private static String wrongState(Running store) throws IOException, InterruptedException {
        Shopper mallory = new Shopper();
        Shopper.Page callback = mallory.navigate(store.url() + "/oauth2/authorization/bank", "ana", "demo-password", store.url() + "/login/oauth2/code/bank");
        Shopper anaBrowser = new Shopper();
        anaBrowser.once(store.url() + "/oauth2/authorization/bank");
        Shopper.Page result = anaBrowser.get(callback.url());
        return result.url().contains("error") || result.status() >= 400 ? "refused (no sign-in started by this browser has that state)" : "accepted";
    }

    private static String refusal(java.util.concurrent.Callable<Object> attempt) {
        try {
            attempt.call();
            return "accepted";
        } catch (Exception e) {
            String text = String.valueOf(e.getMessage()).replace("An error occurred while attempting to decode the Jwt: ", "").replaceAll("\\s+", " ");
            if (text.startsWith("Jwt expired at")) {
                text = "expired";
            }
            return "refused (" + text + ")";
        }
    }

    private static JwtDecoder labDecoder(LocalProvider provider) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("lab").clientId(LocalProvider.CLIENT).clientSecret("unused")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri("http://127.0.0.1/cb").authorizationUri("https://bank.example/auth")
                .tokenUri("https://bank.example/token").jwkSetUri(provider.jwksUri()).issuerUri(LocalProvider.ISSUER).scope("openid").build();
        OidcIdTokenDecoderFactory factory = new OidcIdTokenDecoderFactory();
        factory.setJwtValidatorFactory(r -> {
            OidcIdTokenValidator oidc = new OidcIdTokenValidator(r);
            oidc.setClockSkew(Duration.ofSeconds(30));
            return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ofSeconds(30)), oidc);
        });
        return factory.createDecoder(registration);
    }

    private static void act2CheckTheIdToken(StringBuilder out, Running store, LocalProvider p) throws JOSEException {
        line(out, "TWO. Checking the ID token, as OpenID Connect Core requires.");
        JwtDecoder decoder = labDecoder(p);
        line(out, "  the bank's own ID token: " + verdict(decoder, p.sign(p.claims().build())));
        line(out, "  the same claims signed with a key the bank never published, under the bank's key id: " + verdict(decoder, p.signWithAnotherKey(p.claims().build())));
        line(out, "  issuer https://bank.example/ with one extra slash: " + verdict(decoder, p.sign(p.claims().issuer("https://bank.example/").build())));
        line(out, "  issuer https://wallet.example, signed by the bank: " + verdict(decoder, p.sign(p.claims().issuer("https://wallet.example").build())));
        line(out, "  audience budget-app: " + verdict(decoder, p.sign(p.claims().audience("budget-app").build())));
        JWTClaimsSet.Builder two = p.claims().audience(List.of("store-web", "budget-app"));
        line(out, "  audiences store-web and budget-app, with no azp: " + verdict(decoder, p.sign(two.build())) + "; with azp store-web: " + verdict(decoder, p.sign(two.claim("azp", "store-web").build())));
        line(out, "  29 seconds past its expiry, inside the 30 second skew: " + verdict(decoder, p.sign(p.expiredSecondsAgo(29).build())) + "; 31 seconds past: " + verdict(decoder, p.sign(p.expiredSecondsAgo(31).build())));
        line(out, "  no subject: " + verdict(decoder, p.sign(p.claims().subject(null).build())));
        line(out, "  a header saying alg none: " + verdict(decoder, p.unsigned(p.claims().build())));
        line(out, "  the nonce is checked by the sign-in itself, against the one kept in the browser's session, and the state with it: see one");
    }

    private static String verdict(JwtDecoder decoder, String token) {
        String r = refusal(() -> decoder.decode(token));
        return r.equals("accepted") ? "accepted" : r;
    }

    private static void act3DiscoveryAndKeys(StringBuilder out, KeycloakLab keycloak, Running store, Tokens tokens, LocalProvider p) throws Exception {
        line(out, "THREE. Discovery and keys.");
        line(out, "  the store is configured with one value, the issuer <keycloak>/realms/bank");
        ClientRegistration found = ClientRegistrations.fromIssuerLocation(keycloak.issuer("bank")).clientId("store-web").build();
        line(out, "  it reads <keycloak>/realms/bank/.well-known/openid-configuration, which names the issuer it asked for: " + keycloak.issuer("bank").equals(found.getProviderDetails().getIssuerUri()));
        line(out, "  the authorization, token and key addresses all start with the issuer's own address: "
                + (found.getProviderDetails().getAuthorizationUri().startsWith(keycloak.base()) && found.getProviderDetails().getTokenUri().startsWith(keycloak.base())
                        && found.getProviderDetails().getJwkSetUri().startsWith(keycloak.base())));
        p.serveDiscovery("https://wallet.example", p.jwksUri());
        line(out, "  a document at that address naming the issuer https://wallet.example: " + refusalOf(() -> ClientRegistrations.fromIssuerLocation(p.issuerAddress()).clientId("x").build()));
        p.serveDiscovery(p.issuerAddress(), "https://keys.example/jwks.json");
        ClientRegistration elsewhere = ClientRegistrations.fromIssuerLocation(p.issuerAddress()).clientId("x").build();
        line(out, "  a document whose keys are on https://keys.example: accepted by Spring Security: " + "https://keys.example/jwks.json".equals(elsewhere.getProviderDetails().getJwkSetUri())
                + "  <- IT CHECKS THE ISSUER, NOT WHERE THE KEYS ARE: THE ISSUER ADDRESS MUST BE https AND TRUSTED");
        int before = LocalProvider.kidsOf(keycloak.certs("bank")).size();
        JwtDecoder decoder = new OidcIdTokenDecoderFactory().createDecoder(store.bean(ClientRegistrationRepository.class).findByRegistrationId("bank"));
        String oldToken = tokens.signIn("bank", "ana", "openid email").string("id_token");
        line(out, "  Keycloak publishes " + before + " signing key; a token signed with it: " + refusal(() -> decoder.decode(oldToken)));
        keycloak.addSigningKey("bank");
        int after = LocalProvider.kidsOf(keycloak.certs("bank")).size();
        String newToken = tokens.signIn("bank", "ana", "openid email").string("id_token");
        line(out, "  the bank adds a second key and signs new tokens with it; it now publishes " + after + ", and the new token names a key id the store has not seen: "
                + !Tokens.header(newToken).get("kid").equals(Tokens.header(oldToken).get("kid")));
        line(out, "  the store reloads the key set on its own: a token signed with the old key: " + refusal(() -> decoder.decode(oldToken)) + "; with the new key: " + refusal(() -> decoder.decode(newToken)));
        line(out, "  over real HTTP on this machine: metadata and keys loaded: true, an ID token from that provider accepted: " + !refusal(() -> decoder.decode(newToken)).startsWith("refused"));
    }

    private static String refusalOf(java.util.concurrent.Callable<Object> attempt) {
        try {
            attempt.call();
            return "accepted";
        } catch (Exception e) {
            return e.getMessage() != null && e.getMessage().startsWith("The Issuer") ? "refused (the metadata names another issuer)  <- ONE PROVIDER CANNOT POSE AS ANOTHER" : "refused (" + e.getMessage() + ")";
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
        line(out, "  the bank says " + anaMe.get("subject") + " has the e-mail address " + anaMe.get("email") + ", verified: " + anaMe.get("emailVerified"));
        line(out, "  the wallet says its user " + malMe.get("subject") + " also has the e-mail address " + malMe.get("email") + ", verified: " + malMe.get("emailVerified"));
        line(out, "  a store that keys accounts by e-mail address: Mallory signs in with the wallet and gets: " + json(mallory.get(store.url() + "/account?keyedBy=email").body()).get("account") + "  <- ACCOUNT TAKEOVER");
        line(out, "  keyed by issuer and subject: Ana gets " + json(ana.get(store.url() + "/account?keyedBy=subject").body()).get("account") + ", Mallory gets "
                + json(mallory.get(store.url() + "/account?keyedBy=subject").body()).get("account"));
        Tokens.Reply reply = tokens.signIn("bank", "ana", "openid email");
        HttpResponse<String> info = tokens.userinfo(reply.string("access_token"));
        line(out, "  userinfo with Ana's access token: " + info.statusCode() + ", sub " + json(info.body()).get("sub") + ", the same subject as the ID token: " + json(info.body()).get("sub").equals(Tokens.claims(reply.string("id_token")).get("sub")));
        line(out, "  userinfo with the ID token instead: " + (tokens.userinfo(reply.string("id_token")).statusCode() == 401 ? "refused (401)" : "accepted"));
    }

    private static void act5SigningOut(StringBuilder out, KeycloakLab keycloak, Running store, Tokens tokens) throws Exception {
        line(out, "FIVE. Signing out, and the bill.");
        Shopper ana = new Shopper();
        ana.navigate(store.url() + "/oauth2/authorization/bank", "ana", "demo-password");
        String csrf = String.valueOf(json(ana.get(store.url() + "/csrf").body()).get("token"));
        Shopper.Page logout = ana.post(store.url() + "/logout", "_csrf=" + Shopper.enc(csrf));
        line(out, "  Ana signs out at the store: the store's session ended: " + (ana.get(store.url() + "/me").status() == 401));
        line(out, "  her browser is sent to <keycloak>/realms/bank/protocol/openid-connect/logout with id_token_hint, post_logout_redirect_uri: "
                + (logout.url().contains("/protocol/openid-connect/logout") && logout.url().contains("id_token_hint=") && logout.url().contains("post_logout_redirect_uri=")));
        Shopper.Page back = ana.navigate(logout.url(), null, null);
        line(out, "  Keycloak ends its session and sends the browser back to the store: " + back.url().startsWith(store.url() + "/"));
        Shopper.Page again = ana.get(store.url() + "/oauth2/authorization/bank");
        line(out, "  signing in again asks for her password again: " + again.body().contains("kc-form-login"));

        Shopper laptop = new Shopper();
        Shopper phone = new Shopper();
        laptop.navigate(store.url() + "/oauth2/authorization/bank", "ana", "demo-password");
        phone.navigate(store.url() + "/oauth2/authorization/bank", "ana", "demo-password");
        String laptopSession = String.valueOf(json(laptop.get(store.url() + "/me").body()).get("sid"));
        String phoneSession = String.valueOf(json(phone.get(store.url() + "/me").body()).get("sid"));
        line(out, "  Ana is signed in on a laptop and a phone: two Keycloak sessions: " + !laptopSession.equals(phoneSession));
        int ended = keycloak.endSession("bank", laptopSession);
        boolean laptopOut = false;
        for (int i = 0; i < 40 && !laptopOut; i++) {
            laptopOut = laptop.get(store.url() + "/me").status() == 401;
            if (!laptopOut) {
                Thread.sleep(250);
            }
        }
        line(out, "  Keycloak ends the laptop's session (" + ended + ") and sends the store a back-channel logout token: the laptop signed in: " + !laptopOut
                + ", the phone signed in: " + (phone.get(store.url() + "/me").status() == 200));
        Tokens.Reply reply = tokens.signIn("bank", "ana", "openid email");
        java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
        for (String[] bad : List.of(new String[] {"a made-up logout token", "not.a.token"}, new String[] {"an ID token sent as a logout token", reply.string("id_token")})) {
            int status = http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(store.url() + "/logout/connect/back-channel/bank")).header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString("logout_token=" + Shopper.enc(bad[1]))).build(), java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
            line(out, "  " + bad[0] + ": refused (the store answered " + status + ")" + ", the phone still signed in: " + (phone.get(store.url() + "/me").status() == 200));
        }
        line(out, "  OpenID Connect has many checks and options, so production code uses a certified library and a real provider");
        line(out, "  the ID token is for the store only: never send it to an API, and never take an access token as a sign-in");
        line(out, "  signing out must reach every party: the store, the provider, and through back-channel logout every other application");
    }
}
