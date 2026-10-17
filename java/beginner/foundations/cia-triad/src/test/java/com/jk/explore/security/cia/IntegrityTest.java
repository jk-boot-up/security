package com.jk.explore.security.cia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IntegrityTest {

    @Test
    void aSealMatchesTheOrderItWasMadeFor() {
        Seal seal = new Seal("test-secret");
        assertTrue(seal.matches(CiaDemo.MAYA, seal.of(CiaDemo.MAYA)));
        assertEquals(64, seal.of(CiaDemo.MAYA).length());
    }

    @Test
    void changingOneFieldBreaksTheSeal() {
        Seal seal = new Seal("test-secret");
        String original = seal.of(CiaDemo.MAYA);
        assertFalse(seal.matches(CiaDemo.MAYA.withAmount(12), original));
    }

    @Test
    void aDifferentKeyMakesADifferentSeal() {
        assertFalse(new Seal("one").of(CiaDemo.MAYA).equals(new Seal("two").of(CiaDemo.MAYA)));
    }

    @Test
    void aChangeMadeBehindTheApplicationIsCaughtOnRead() {
        SecureStore store = new SecureStore(new Seal("test-secret"));
        store.save(CiaDemo.MAYA);
        SecureStore.Stored stored = store.storageForDemo().get("O-101");
        store.storageForDemo().put("O-101", new SecureStore.Stored(stored.order().withAmount(12), stored.seal()));
        assertThrows(TamperedOrderException.class, () -> store.read(CiaDemo.ANA, "O-101"));
    }

    @Test
    void aLegitimateChangeIsSealedAgain() {
        SecureStore store = new SecureStore(new Seal("test-secret"));
        store.save(CiaDemo.MAYA);
        store.setAmount(CiaDemo.ANA, "O-101", 1100);
        assertEquals(1100, store.read(CiaDemo.ANA, "O-101").amountDollars());
    }
}
