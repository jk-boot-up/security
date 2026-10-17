package com.jk.explore.security.cia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AvailabilityTest {

    @Test
    void theLimitKeepsRoomForOtherClients() {
        TickServer.Result result = new TickServer(CiaDemo.SERVER_CAPACITY)
                .serve(CiaDemo.flood(), new RateLimiter(CiaDemo.PER_CLIENT_LIMIT));
        assertEquals(3, result.served().get("bot"));
        assertEquals(97, result.refused().get("bot"));
        assertEquals(1, result.served().get("maya"));
        assertEquals(4, result.totalServed());
    }

    @Test
    void aLimitOfZeroServesNobody() {
        TickServer.Result result = new TickServer(10).serve(CiaDemo.flood(), new RateLimiter(0));
        assertEquals(0, result.totalServed());
    }
}
