package com.jk.explore.security.authkeycloak;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Runs the online store story in five numbered acts against a real Spring Boot resource server on the loopback interface.
 * The tokens come from a stand-in for Keycloak, so the demo needs no container; the integration tests use the real Keycloak.
 */
public class KeycloakDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static JwtDecoder decoder(Issuer issuer, SimClock clock, List<OAuth2TokenValidator<Jwt>> extra) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(issuer.publicKey()).build();
        decoder.setJwtValidator(Validators.all(Issuer.REALM_ISSUER, Issuer.AUDIENCE, clock, extra));
        return decoder;
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Keys are random, so only facts that do not depend on them are printed. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        SimClock clock = new SimClock(Instant.parse("2026-01-01T09:00:00Z"));
        Issuer issuer = new Issuer(Issuer.REALM_ISSUER, clock);
        try (Running api = Running.start(decoder(issuer, clock, List.of()))) {
            act1Tokens(out, api, issuer, clock);
            act2Roles(out, api, issuer);
            act3IssuerAndAudience(out, api, issuer);
        }
        act4StaleRoles(out, issuer, clock);
        act5Revocation(out, issuer, clock);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Tokens(StringBuilder out, Running api, Issuer issuer, SimClock clock) throws IOException, InterruptedException {
        line(out, "ONE. No login in the shop, only tokens.");
        line(out, "  no token: " + api.status("/me", null) + ", garbage as a token: " + api.status("/me", "not-a-token")
                + ", a token signed with another key: " + api.status("/me", issuer.forge("ana", List.of("customer"))));
        String token = issuer.mint("ana", List.of("customer"), 300, Issuer.AUDIENCE);
        line(out, "  a token the login server signed, good for 300 seconds: " + api.status("/me", token));
        clock.advanceSeconds(301);
        line(out, "  the same token 301 seconds later: " + api.status("/me", token) + "  <- EXPIRED, AND THE SHOP NEEDED NO CALL TO THE LOGIN SERVER TO KNOW");
    }

    private static void act2Roles(StringBuilder out, Running api, Issuer issuer) throws IOException, InterruptedException {
        line(out, "TWO. Who is it, and what may they do.");
        String ana = issuer.mint("ana", List.of("customer"), 300, Issuer.AUDIENCE);
        String sam = issuer.mint("sam", List.of("support"), 300, Issuer.AUDIENCE);
        String root = issuer.mint("root", List.of("admin"), 300, Issuer.AUDIENCE);
        line(out, "  the roles in realm_access.roles become authorities: " + authorities(api, ana) + ", " + authorities(api, sam) + ", " + authorities(api, root));
        line(out, "  Ana reads her own order A-1: " + api.status("/orders/A-1", ana) + ", and Ben's order B-2: " + api.status("/orders/B-2", ana) + "  <- LOGGED IN, NOT ALLOWED");
        line(out, "  support Sam reads Ben's order B-2: " + api.status("/orders/B-2", sam));
        line(out, "  the sales report: customer " + api.status("/admin/report", ana) + ", support " + api.status("/admin/report", sam) + ", admin " + api.status("/admin/report", root));
        line(out, "  401 means the shop does not know who is asking, 403 means it knows and the answer is no");
    }

    private static String authorities(Running api, String token) throws IOException, InterruptedException {
        return api.get("/me", token).body();
    }

    private static void act3IssuerAndAudience(StringBuilder out, Running api, Issuer issuer) throws IOException, InterruptedException {
        line(out, "THREE. A real signature is not enough.");
        String other = issuer.mintWithIssuer("ana", List.of("customer"), 300, Issuer.AUDIENCE, "http://login.elsewhere.example/realms/other");
        String wrongAudience = issuer.mint("ana", List.of("customer"), 300, "another-api");
        line(out, "  a correctly signed token from another issuer: " + api.status("/me", other));
        line(out, "  a correctly signed token meant for another API: " + api.status("/me", wrongAudience) + "  <- WITHOUT AN AUDIENCE CHECK, ANY API'S TOKEN OPENS THIS ONE");
        line(out, "  the shop checks the signature, the expiry, the issuer and the audience");
    }

    private static void act4StaleRoles(StringBuilder out, Issuer issuer, SimClock clock) throws IOException, InterruptedException {
        line(out, "FOUR. The roles in a token are a copy.");
        Map<String, List<String>> directory = new HashMap<>();
        directory.put("root", List.of("admin"));
        try (Running api = Running.start(decoder(issuer, clock, List.of()))) {
            String token = issuer.mint("root", directory.get("root"), 300, Issuer.AUDIENCE);
            line(out, "  root, an admin, opens the sales report: " + api.status("/admin/report", token));
            directory.put("root", List.of("customer"));
            line(out, "  root is demoted to customer in the login server's directory");
            line(out, "  the token root already holds still opens the report: " + api.status("/admin/report", token) + "  <- THE SHOP ONLY SEES THE COPY");
            clock.advanceSeconds(301);
            line(out, "  after the token expires: " + api.status("/admin/report", token));
            String fresh = issuer.mint("root", directory.get("root"), 300, Issuer.AUDIENCE);
            line(out, "  a new token carries the new roles: report " + api.status("/admin/report", fresh) + ", own pages " + api.status("/me", fresh));
            line(out, "  so a short lifetime is the control: the stale window is at most the lifetime of the token");
        }
    }

    private static void act5Revocation(StringBuilder out, Issuer issuer, SimClock clock) throws IOException, InterruptedException {
        line(out, "FIVE. Ending a token early, and the bill.");
        Revocations revocations = new Revocations();
        try (Running api = Running.start(decoder(issuer, clock, List.of(revocations)))) {
            String token = issuer.mint("root", List.of("admin"), 300, Issuer.AUDIENCE);
            line(out, "  an admin token: " + api.status("/admin/report", token) + " and " + api.status("/admin/report", token));
            revocations.revoke(issuer.lastTokenId());
            line(out, "  its id is put on the revocation list: " + api.status("/admin/report", token) + "  <- ENDED AT ONCE");
            line(out, "  the price: the list was consulted " + revocations.lookups() + " times for 3 requests, once for every request");
        }
        line(out, "  without a list the shop made no call at all, and the token could be used until it expired");
        line(out, "  refresh tokens give long sessions with short access tokens, and the login server can refuse to refresh");
        line(out, "  the login server is now one more system that must be kept running, patched and backed up");
    }
}
