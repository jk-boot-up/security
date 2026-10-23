package com.jk.explore.security.pqc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import org.junit.jupiter.api.Test;

class SignaturesTest {

    @Test
    void signsVerifiesAndRejectsChanges() throws Exception {
        KeyPair pair = Signatures.generate("ML-DSA-65");
        byte[] m = Signatures.statement();
        byte[] sig = Signatures.sign(pair, m);
        assertTrue(Signatures.verify(pair, m, sig));
        byte[] changed = m.clone();
        changed[0] ^= 1;
        assertFalse(Signatures.verify(pair, changed, sig));
        assertFalse(Signatures.verify(Signatures.generate("ML-DSA-65"), m, sig));
    }
}
