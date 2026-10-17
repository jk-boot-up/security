package com.jk.explore.security.dns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResolverTest {

    private static final String NAME = "shop.example.com";

    private Resolver resolver(long cap) {
        return new Resolver(TransactionIds.keyed("test-secret"), cap);
    }

    private boolean answer(Resolver resolver, Resolver.Pending pending, int id, int port, long ttl, int address) {
        return resolver.accept(pending, DnsMessage.query(id, NAME).answeredWith(ttl, address), port, 0);
    }

    @Test
    void matchingAnswerIsCachedUntilItsTtlEnds() {
        Resolver resolver = resolver(3600);
        Resolver.Pending pending = resolver.ask(NAME, 0).orElseThrow();
        assertTrue(answer(resolver, pending, pending.id(), pending.port(), 300, 5));
        assertEquals(5, resolver.cached(NAME, 299).orElseThrow());
        assertTrue(resolver.cached(NAME, 300).isEmpty());
        assertTrue(resolver.ask(NAME, 100).isEmpty());
        assertTrue(resolver.ask(NAME, 300).isPresent());
    }

    @Test
    void wrongIdOrWrongPortIsNotBelieved() {
        Resolver resolver = resolver(3600);
        Resolver.Pending pending = resolver.ask(NAME, 0).orElseThrow();
        assertFalse(answer(resolver, pending, pending.id() ^ 1, pending.port(), 300, 5));
        assertFalse(answer(resolver, pending, pending.id(), pending.port() + 1, 300, 5));
        assertTrue(resolver.cached(NAME, 1).isEmpty());
    }

    @Test
    void wrongQuestionOrAQueryIsNotBelieved() {
        Resolver resolver = resolver(3600);
        Resolver.Pending pending = resolver.ask(NAME, 0).orElseThrow();
        assertFalse(resolver.accept(pending, DnsMessage.query(pending.id(), "other.example.com").answeredWith(300, 5), pending.port(), 0));
        assertFalse(resolver.accept(pending, DnsMessage.query(pending.id(), NAME), pending.port(), 0));
    }

    @Test
    void lifetimeIsCapped() {
        Resolver resolver = resolver(3600);
        Resolver.Pending pending = resolver.ask(NAME, 0).orElseThrow();
        answer(resolver, pending, pending.id(), pending.port(), 86400, 5);
        assertTrue(resolver.cached(NAME, 3599).isPresent());
        assertTrue(resolver.cached(NAME, 3600).isEmpty());
    }

    @Test
    void sequentialIdsCanBeGuessedAndKeyedOnesCannotBeFromEarlierOnes() {
        TransactionIds sequential = TransactionIds.sequential(10, 53);
        int first = sequential.nextId();
        assertEquals(first + 1, sequential.nextId());
        assertEquals(53, sequential.nextPort());
        TransactionIds keyed = TransactionIds.keyed("test-secret");
        int a = keyed.nextId();
        int portA = keyed.nextPort();
        int b = keyed.nextId();
        assertNotEquals(a + 1, b);
        assertTrue(portA >= TransactionIds.LOWEST_PORT && keyed.nextPort() >= TransactionIds.LOWEST_PORT);
    }

    @Test
    void guessingAKeyedIdAndPortFails() {
        Resolver resolver = resolver(3600);
        Resolver.Pending pending = resolver.ask(NAME, 0).orElseThrow();
        boolean hit = false;
        for (int i = 0; i < 1000; i++) {
            hit |= answer(resolver, pending, i, 33333, 300, 99);
        }
        assertFalse(hit);
    }

    @Test
    void guessingASequentialIdWorksFirstTime() {
        Resolver resolver = new Resolver(TransactionIds.sequential(500, 53), 3600);
        resolver.ask("other.example.com", 0);
        Resolver.Pending pending = resolver.ask(NAME, 0).orElseThrow();
        assertTrue(answer(resolver, pending, 501, 53, 300, 99));
    }

    @Test
    void realUdpExchangeCarriesTheNameInTheClear() throws Exception {
        LoopbackExchange.Result result = LoopbackExchange.ask(NAME, 0x1234, 300, Address.parse("203.0.113.10"));
        assertTrue(result.answered());
        assertTrue(result.idRepeated());
        assertEquals("203.0.113.10", Address.text(result.address()));
        assertTrue(result.nameReadableOnTheWire());
    }
}
