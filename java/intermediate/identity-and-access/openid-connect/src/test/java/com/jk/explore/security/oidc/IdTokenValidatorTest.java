package com.jk.explore.security.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdTokenValidatorTest {

    private static final String NONCE = "nonce-1";
    private OidcDemo.World world;
    private ProviderConfig config;
    private Map<String, Object> good;

    @BeforeEach
    void setUp() throws Exception {
        world = new OidcDemo.World();
        config = world.discover(OidcDemo.BANK);
        good = world.bank.sampleIdClaims(OidcDemo.ANA, Store.CLIENT_ID, NONCE);
    }

    private IdToken validate(Map<String, Object> claims) throws InvalidTokenException {
        return IdTokenValidator.validate(world.bank.sign("JWT", claims), config, Store.CLIENT_ID, NONCE, world.clock.now());
    }

    private String refusal(Map<String, Object> claims) {
        return assertThrows(InvalidTokenException.class, () -> validate(claims)).getMessage();
    }

    @Test
    void aGoodTokenGivesIssuerAndSubject() throws Exception {
        IdToken token = validate(good);
        assertEquals("https://bank.example | ana-0001", token.identity());
        assertEquals(List.of("pwd", "otp"), token.amr());
    }

    @Test
    void ed25519KeysMatchTheRfc8032Example() {
        byte[] seed = HexFormat.of().parseHex("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60");
        byte[] expected = HexFormat.of().parseHex("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a");
        assertEquals(HexFormat.of().formatHex(expected), HexFormat.of().formatHex(Ed25519.encode(Ed25519.fromSeed(seed).getPublic())));
    }

    @Test
    void issuerMustMatchExactly() {
        assertEquals("issuer is not the configured issuer", refusal(OidcDemo.with(good, "iss", "https://bank.example/")));
        assertEquals("issuer is not the configured issuer", refusal(OidcDemo.with(good, "iss", "https://BANK.example")));
    }

    @Test
    void audienceAndAzpRules() throws Exception {
        assertEquals("audience does not include this client", refusal(OidcDemo.with(good, "aud", "budget-app")));
        Map<String, Object> two = OidcDemo.with(good, "aud", List.of(Store.CLIENT_ID, "budget-app"));
        assertEquals("several audiences and no azp naming this client", refusal(two));
        validate(OidcDemo.with(two, "azp", Store.CLIENT_ID));
        assertEquals("azp names another client", refusal(OidcDemo.with(good, "azp", "budget-app")));
    }

    @Test
    void expiryWithSkewAndAge() throws Exception {
        long now = world.clock.now();
        validate(OidcDemo.with(OidcDemo.with(good, "iat", now - 329), "exp", now - 29));
        assertEquals("expired", refusal(OidcDemo.with(OidcDemo.with(good, "iat", now - 330), "exp", now - 30)));
        assertEquals("issued in the future", refusal(OidcDemo.with(good, "iat", now + 31)));
        assertEquals("issued too long ago", refusal(OidcDemo.with(OidcDemo.with(good, "iat", now - 601), "exp", now + 100)));
    }

    @Test
    void nonceMustBeTheOneForThisBrowser() {
        assertEquals("nonce is not the one this browser was given", refusal(OidcDemo.with(good, "nonce", "other")));
        Map<String, Object> none = new LinkedHashMap<>(good);
        none.remove("nonce");
        assertEquals("no nonce", refusal(none));
    }

    @Test
    void otherKindsOfTokenAreRefused() {
        String access = world.bank.sign("at+jwt", good);
        assertThrows(InvalidTokenException.class, () -> IdTokenValidator.validate(access, config, Store.CLIENT_ID, NONCE, world.clock.now()));
        String logout = world.bank.sign("logout+jwt", good);
        assertThrows(InvalidTokenException.class, () -> IdTokenValidator.validate(logout, config, Store.CLIENT_ID, NONCE, world.clock.now()));
    }

    @Test
    void unknownKeyAndForeignSignatureAreRefused() {
        String unknown = Provider.signWith("no-such-key", OidcDemo.BANK_KEY_A, "JWT", good);
        assertEquals("unknown key id", assertThrows(InvalidTokenException.class,
                () -> IdTokenValidator.validate(unknown, config, Store.CLIENT_ID, NONCE, world.clock.now())).getMessage());
        String foreign = Provider.signWith("bank-2026-a", OidcDemo.STRANGER_KEY, "JWT", good);
        assertEquals("signature does not match", assertThrows(InvalidTokenException.class,
                () -> IdTokenValidator.validate(foreign, config, Store.CLIENT_ID, NONCE, world.clock.now())).getMessage());
    }

    @Test
    void aChangedPayloadBreaksTheSignature() {
        String token = world.bank.sign("JWT", good);
        String[] parts = token.split("\\.");
        String other = world.bank.sign("JWT", OidcDemo.with(good, "sub", "mallory"));
        String spliced = parts[0] + "." + other.split("\\.")[1] + "." + parts[2];
        assertEquals("signature does not match", assertThrows(InvalidTokenException.class,
                () -> IdTokenValidator.validate(spliced, config, Store.CLIENT_ID, NONCE, world.clock.now())).getMessage());
    }
}
