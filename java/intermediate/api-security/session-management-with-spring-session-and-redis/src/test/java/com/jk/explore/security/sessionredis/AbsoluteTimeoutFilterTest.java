package com.jk.explore.security.sessionredis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** The absolute-timeout decision is driven by a clock and the session's age, so it tests with a fixed clock and Spring's mock request; no Redis, no container. */
class AbsoluteTimeoutFilterTest {

    private static final Duration LIMIT = Duration.ofHours(8);

    @Test
    void endsaSessionThatIsOlderThanTheLimit() throws Exception {
        Clock nineHoursOn = Clock.fixed(Instant.now().plus(Duration.ofHours(9)), ZoneOffset.UTC);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(); // created "now"; nine hours on it is past the eight-hour limit
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new AbsoluteTimeoutFilter(nineHoursOn, LIMIT).doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertNull(chain.getRequest(), "the request is not passed on once the session is ended");
        assertNull(request.getSession(false), "the session is invalidated");
    }

    @Test
    void letsaFreshSessionThrough() throws Exception {
        Clock now = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new AbsoluteTimeoutFilter(now, LIMIT).doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertNotNull(chain.getRequest(), "a fresh session is passed on to the rest of the chain");
    }

    @Test
    void doesNothingWhenThereIsNoSession() throws Exception {
        Clock farFuture = Clock.fixed(Instant.now().plus(Duration.ofDays(3650)), ZoneOffset.UTC);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new AbsoluteTimeoutFilter(farFuture, LIMIT).doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "with no session there is nothing to time out");
    }

    @Test
    void offsetClockMovesForwardByTheGivenAmount() {
        OffsetClock clock = new OffsetClock();
        Instant before = clock.instant();
        clock.advance(Duration.ofHours(8));
        assertTrue(Duration.between(before, clock.instant()).toHours() >= 7, "the clock jumped about eight hours");
        assertEquals(ZoneOffset.UTC, clock.getZone());
    }
}
