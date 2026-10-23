package com.jk.explore.security.pqc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InteropTest {

    @Test
    void kemWorksBothWays() throws Exception {
        assertTrue(Interop.jdkEncapsulatesBouncyCastleDecapsulates());
        assertTrue(Interop.bouncyCastleEncapsulatesJdkDecapsulates());
        assertTrue(Interop.publicKeyEncodingsAgree());
    }

    @Test
    void signaturesWorkBothWays() throws Exception {
        assertTrue(Interop.jdkSignsBouncyCastleVerifies(Signatures.statement()));
        assertTrue(Interop.bouncyCastleSignsJdkVerifies(Signatures.statement()));
    }

    @Test
    void privateKeyEncodingsAreNotInterchangeable() throws Exception {
        assertFalse(Interop.jdkReadsBouncyCastlePrivateKey());
    }
}
