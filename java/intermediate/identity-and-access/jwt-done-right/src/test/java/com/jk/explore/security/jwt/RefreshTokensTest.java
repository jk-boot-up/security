package com.jk.explore.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RefreshTokensTest {

    @Test
    void aRefreshTokenWorksUntilItsOwnerIsRevoked() {
        RefreshTokens tokens = new RefreshTokens();
        String ana = tokens.issue("ana");
        String ben = tokens.issue("ben");
        assertEquals("ana", tokens.redeem(ana).orElseThrow());
        tokens.revokeAll("ana");
        assertTrue(tokens.redeem(ana).isEmpty());
        assertEquals("ben", tokens.redeem(ben).orElseThrow());
        assertTrue(tokens.redeem("rt-99").isEmpty());
    }

    @Test
    void anAccessTokenStillWorksAfterRevocationUntilItExpires() throws Exception {
        SimClock clock = new SimClock(JwtDemo.START);
        Validator validator = new Validator(Policy.bank(), JwtDemo.bankKeys(), clock);
        String access = Issuer.issue(JwtDemo.OLD_KID, JwtDemo.OLD_KEY.getPrivate(), "ana", "accounts:read", JwtDemo.START, 300);
        RefreshTokens tokens = new RefreshTokens();
        tokens.revokeAll("ana");
        clock.advance(240);
        assertEquals("ana", validator.validate(access).subject());
    }
}
