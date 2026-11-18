package com.jk.explore.security.sessionredis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** How sessions are named in Redis and in the cookie, checked without Redis. */
class SessionKeysTest {

    private static final String ID = SessionRedisDemo.EXAMPLE_ID;

    @Test
    void theCookieIsTheBase64OfTheIdAndDecodesBack() {
        assertEquals("M2YyYTdjMWUtOWI0ZC00ZThhLWE2ZjAtNWMxZDJlM2I0YTkw", SessionKeys.cookieFor(ID));
        assertEquals(48, SessionKeys.cookieFor(ID).length());
        assertEquals(ID, SessionKeys.idFrom(SessionKeys.cookieFor(ID)));
    }

    @Test
    void aRecordIsLiveOnlyWhileItsExpiryMarkerExists() {
        String other = "00000000-0000-4000-8000-000000000000";
        Set<String> keys = Set.of(SessionKeys.recordKey(ID), SessionKeys.EXPIRES + ID, SessionKeys.recordKey(other),
                "spring:session:expirations:1700000040000");
        assertEquals(Set.of(SessionKeys.recordKey(ID)), SessionKeys.live(keys));
    }

    @Test
    void keysArePrintedWithoutTheIdOrTheTime() {
        assertEquals("spring:session:sessions:expires:<id>", SessionKeys.shown(SessionKeys.EXPIRES + ID, ID));
        assertEquals("spring:session:expirations:<time>", SessionKeys.shown("spring:session:expirations:1700000040000", ID));
        assertEquals("spring:session:index:PRINCIPAL_NAME_INDEX_NAME:ana",
                SessionKeys.shown("spring:session:index:org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME:ana", ID));
    }

    @Test
    void theBanksCookieHasNoProblemsAndABareOneHasFour() {
        assertEquals(List.of(), SessionKeys.cookieProblems("__Host-session=abc; Path=/; Secure; HttpOnly; SameSite=Strict"));
        assertEquals(List.of("no __Host- prefix", "no Secure", "no HttpOnly", "no SameSite=Strict"), SessionKeys.cookieProblems("SESSION=abc; Path=/"));
    }

    @Test
    void base64ByHandGivesTheLibrarysExactResult() {
        assertEquals(List.of("001100", "110110", "011000", "110010"), SessionKeys.sixBitGroups("3f2"));
        assertEquals("M2Yy", SessionKeys.byHand("3f2"));
        String twoChars = ID;
        assertEquals(SessionKeys.cookieFor(twoChars), SessionKeys.byHand(twoChars));
    }
}
