package com.jk.explore.security.symmetric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EcbTest {

    @Test
    void theStatementHasEightBlocksOfWhichThreeAreDifferent() {
        byte[] plain = Hex.bytes(SymmetricDemo.STATEMENT);
        assertEquals(128, plain.length);
        assertEquals(3, EcbCipher.distinctBlocks(plain));
    }

    @Test
    void ecbKeepsTheRepeatsInTheCipherText() {
        byte[] cipher = EcbCipher.encrypt(SymmetricDemo.KEY, Hex.bytes(SymmetricDemo.STATEMENT));
        assertEquals(3, EcbCipher.distinctBlocks(cipher));
    }

    @Test
    void theSameBlockGivesTheSameCipherBlockWhereverItIs() {
        byte[] cipher = EcbCipher.encrypt(SymmetricDemo.KEY, Hex.bytes(SymmetricDemo.STATEMENT));
        assertEquals(Hex.of(java.util.Arrays.copyOfRange(cipher, 16, 32)), Hex.of(java.util.Arrays.copyOfRange(cipher, 32, 48)));
        assertEquals(Hex.of(java.util.Arrays.copyOfRange(cipher, 0, 16)), Hex.of(java.util.Arrays.copyOfRange(cipher, 80, 96)));
    }
}
