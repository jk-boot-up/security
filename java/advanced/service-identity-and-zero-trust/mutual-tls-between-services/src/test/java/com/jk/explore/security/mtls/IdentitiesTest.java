package com.jk.explore.security.mtls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IdentitiesTest {

    private static final int DNS = 2;
    private static final int URI = 6;

    @Test
    void identityIsTheUriInTheSan() {
        assertEquals(Optional.of("payments"), Identities.fromNames(List.of(List.of(DNS, "localhost"), List.of(URI, "spiffe://bank/payments"))));
    }

    @Test
    void aDnsNameIsNotAnIdentity() {
        assertTrue(Identities.fromNames(List.of(List.of(DNS, "payments"))).isEmpty());
    }

    @Test
    void aUriOfAnotherTrustDomainOrWithAPathIsNotAnIdentity() {
        assertTrue(Identities.fromNames(List.of(List.of(URI, "spiffe://other/payments"))).isEmpty());
        assertTrue(Identities.fromNames(List.of(List.of(URI, "spiffe://bank/payments/admin"))).isEmpty());
        assertTrue(Identities.fromNames(List.of(List.of(URI, "spiffe://bank/"))).isEmpty());
    }

    @Test
    void noSanMeansNoIdentity() {
        assertTrue(Identities.fromNames(null).isEmpty());
    }
}
