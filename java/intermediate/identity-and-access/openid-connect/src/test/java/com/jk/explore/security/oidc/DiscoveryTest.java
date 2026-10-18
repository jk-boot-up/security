package com.jk.explore.security.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiscoveryTest {

    @Test
    void metadataAndKeysAreLoadedFromTheConfiguredIssuer() throws Exception {
        OidcDemo.World world = new OidcDemo.World();
        ProviderConfig config = world.discover(OidcDemo.BANK);
        assertEquals("https://bank.example", config.issuer());
        assertEquals("https://bank.example/token", config.tokenEndpoint());
        assertEquals(List.of("bank-2026-a"), config.keys().ids());
    }

    @Test
    void anotherIssuerInTheMetadataIsRefused() throws Exception {
        OidcDemo.World world = new OidcDemo.World();
        Map<String, Object> metadata = Json.parseObject(world.bank.metadata());
        Web web = new Web();
        web.serve(Discovery.metadataUrl(OidcDemo.BANK), () -> Json.write(OidcDemo.with(metadata, "issuer", OidcDemo.WALLET)));
        assertEquals("the metadata names another issuer",
                assertThrows(InvalidTokenException.class, () -> Discovery.discover(OidcDemo.BANK, web::get)).getMessage());
    }

    @Test
    void keysOnAnotherSiteAreRefused() throws Exception {
        OidcDemo.World world = new OidcDemo.World();
        Map<String, Object> metadata = Json.parseObject(world.bank.metadata());
        Web web = new Web();
        web.serve(Discovery.metadataUrl(OidcDemo.BANK), () -> Json.write(OidcDemo.with(metadata, "jwks_uri", "https://keys.example/k")));
        assertThrows(InvalidTokenException.class, () -> Discovery.discover(OidcDemo.BANK, web::get));
    }

    @Test
    void aProviderWithoutEdDsaIsRefused() throws Exception {
        OidcDemo.World world = new OidcDemo.World();
        Map<String, Object> metadata = Json.parseObject(world.bank.metadata());
        Web web = new Web();
        web.serve(Discovery.metadataUrl(OidcDemo.BANK),
                () -> Json.write(OidcDemo.with(metadata, "id_token_signing_alg_values_supported", List.of("RS256"))));
        assertThrows(InvalidTokenException.class, () -> Discovery.discover(OidcDemo.BANK, web::get));
    }

    @Test
    void sameOriginComparesSchemeHostAndPort() {
        assertTrue(Discovery.sameOrigin("https://bank.example", "https://bank.example/jwks.json"));
        assertFalse(Discovery.sameOrigin("https://bank.example", "http://bank.example/jwks.json"));
        assertFalse(Discovery.sameOrigin("https://bank.example", "https://bank.example:8443/jwks.json"));
        assertFalse(Discovery.sameOrigin("https://bank.example", "https://bank.example.evil/jwks.json"));
    }

    @Test
    void discoveryWorksOverRealHttp() throws Exception {
        try (LoopbackProvider loop = new LoopbackProvider(new SimClock(OidcDemo.START), OidcDemo.seed(0x66))) {
            ProviderConfig config = Discovery.discover(loop.provider().issuer(), LoopbackProvider::fetch);
            assertEquals(List.of("loop-1"), config.keys().ids());
        }
    }
}
