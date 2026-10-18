package com.jk.explore.security.bff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TokensTest {

    private final SimClock clock = new SimClock(BffDemo.START);
    private final AuthServer auth = new AuthServer(clock, BffDemo.seeded("bank signing key"));
    private final BankApi api = new BankApi(clock, auth.publicKey());

    private static Request bearer(String url, String token) {
        return Request.of("GET", url).withHeader("Authorization", "Bearer " + token);
    }

    @Test
    void accessTokensExpireAfterFiveMinutes() {
        String token = auth.issue("ana", "accounts.read", null).accessToken();
        assertEquals(200, api.handle(bearer(BankApi.ACCOUNTS, token)).status());
        clock.advance(AuthServer.ACCESS_SECONDS);
        assertEquals(401, api.handle(bearer(BankApi.ACCOUNTS, token)).status());
    }

    @Test
    void aTokenFromAnotherIssuerKeyIsRefused() {
        AuthServer other = new AuthServer(clock, BffDemo.seeded("another key"));
        assertEquals("the token's signature does not match", api.handle(bearer(BankApi.ACCOUNTS, other.issue("ana", "x", null).accessToken())).body());
    }

    @Test
    void refreshTokensRotate() {
        AuthServer.Tokens first = auth.issue("ana", "accounts.read", null);
        AuthServer.Tokens second = auth.refresh(first.refreshToken(), null).orElseThrow();
        assertTrue(auth.refresh(first.refreshToken(), null).isEmpty());
        assertTrue(auth.refresh(second.refreshToken(), null).isPresent());
    }

    @Test
    void aBoundRefreshTokenNeedsItsKey() {
        DpopKey key = new DpopKey(BffDemo.seeded("k"));
        AuthServer.Tokens tokens = auth.issue("ana", "accounts.read", key.thumbprint());
        assertTrue(auth.refresh(tokens.refreshToken(), null).isEmpty());
        assertTrue(auth.refresh(tokens.refreshToken(), new DpopKey(BffDemo.seeded("other")).thumbprint()).isEmpty());
        assertTrue(auth.refresh(tokens.refreshToken(), key.thumbprint()).isPresent());
    }

    @Test
    void aBoundAccessTokenNeedsAProofFromItsKey() {
        DpopKey key = new DpopKey(BffDemo.seeded("k"));
        String token = auth.issue("ana", "accounts.read", key.thumbprint()).accessToken();
        assertEquals(401, api.handle(bearer(BankApi.ACCOUNTS, token)).status());
        Request good = Request.of("GET", BankApi.ACCOUNTS).withHeader("Authorization", "DPoP " + token)
                .withHeader("DPoP", key.proof("GET", BankApi.ACCOUNTS, clock.now(), token));
        assertEquals(200, api.handle(good).status());
    }

    @Test
    void anUnboundTokenStillWorksAsABearerToken() {
        String token = auth.issue("ana", "accounts.read", null).accessToken();
        assertEquals(200, api.handle(Request.of("GET", BankApi.ACCOUNTS).withHeader("Authorization", "Bearer " + token)).status());
        assertEquals(401, api.handle(Request.of("GET", BankApi.ACCOUNTS).withHeader("Authorization", "Basic " + token)).status());
    }
}
