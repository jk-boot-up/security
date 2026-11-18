package com.jk.explore.security.workloadid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.spiffe.spiffeid.SpiffeId;
import org.junit.jupiter.api.Test;

class SpiffeIdsTest {

    @Test
    void takesAnIdApartIntoTrustDomainAndPath() {
        SpiffeIds.Parts p = SpiffeIds.parts("spiffe://bank.example/payments");
        assertEquals("bank.example", p.trustDomain());
        assertEquals("/payments", p.path());
    }

    @Test
    void agreesWithTheJavaSpiffeLibrary() {
        for (String id : new String[] {"spiffe://bank.example/ledger", "spiffe://bank.example/node/lab"}) {
            SpiffeId library = SpiffeId.parse(id);
            SpiffeIds.Parts mine = SpiffeIds.parts(id);
            assertEquals(library.getTrustDomain().getName(), mine.trustDomain());
            assertEquals(library.getPath(), mine.path());
        }
    }

    @Test
    void refusesAnythingElse() {
        assertThrows(IllegalArgumentException.class, () -> SpiffeIds.parts("https://bank.example/payments"));
    }
}
