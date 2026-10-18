package com.jk.explore.security.signing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MerkleTreeTest {

    private static List<byte[]> leaves(int n) {
        List<byte[]> leaves = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            leaves.add(("entry " + i).getBytes(StandardCharsets.UTF_8));
        }
        return leaves;
    }

    @Test
    void knownValuesFromTheDefinition() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Digests.hex(MerkleTree.root(List.of(), 0)));
        assertEquals("6e340b9cffb37a989ca544e6bb780a2c78901d3fb33738768511a30617afa01d", Digests.hex(MerkleTree.leafHash(new byte[0])));
        List<byte[]> two = leaves(2);
        assertEquals(Digests.hex(MerkleTree.nodeHash(MerkleTree.leafHash(two.get(0)), MerkleTree.leafHash(two.get(1)))),
                Digests.hex(MerkleTree.root(two, 2)));
    }

    @Test
    void theTreeSplitsAtTheLargestPowerOfTwoBelowItsSize() {
        assertEquals(1, MerkleTree.split(2));
        assertEquals(2, MerkleTree.split(3));
        assertEquals(4, MerkleTree.split(5));
        assertEquals(4, MerkleTree.split(8));
        assertEquals(8, MerkleTree.split(9));
    }

    @Test
    void everyInclusionProofChecksAndAWrongOneDoesNot() {
        List<byte[]> leaves = leaves(17);
        for (int n = 1; n <= 17; n++) {
            byte[] root = MerkleTree.root(leaves, n);
            for (int m = 0; m < n; m++) {
                List<byte[]> proof = MerkleTree.inclusionProof(leaves, m, n);
                byte[] leaf = MerkleTree.leafHash(leaves.get(m));
                assertTrue(MerkleTree.verifyInclusion(leaf, m, n, proof, root), "leaf " + m + " of " + n);
                assertFalse(MerkleTree.verifyInclusion(MerkleTree.leafHash(new byte[] {9}), m, n, proof, root));
                if (n > 1) {
                    assertFalse(MerkleTree.verifyInclusion(leaf, (m + 1) % n, n, proof, root), "wrong index " + m + " of " + n);
                }
            }
        }
    }

    @Test
    void everyConsistencyProofChecksAndAChangedHistoryDoesNot() {
        List<byte[]> leaves = leaves(17);
        for (int n = 1; n <= 17; n++) {
            for (int m = 1; m <= n; m++) {
                List<byte[]> proof = MerkleTree.consistencyProof(leaves, m, n);
                assertTrue(MerkleTree.verifyConsistency(m, MerkleTree.root(leaves, m), n, MerkleTree.root(leaves, n), proof), m + " to " + n);
                if (m < n) {
                    List<byte[]> rewritten = new ArrayList<>(leaves);
                    rewritten.set(m - 1, "rewritten".getBytes(StandardCharsets.UTF_8));
                    assertFalse(MerkleTree.verifyConsistency(m, MerkleTree.root(leaves, m), n, MerkleTree.root(rewritten, n), proof),
                            "rewritten history " + m + " to " + n);
                }
            }
        }
    }

    @Test
    void impossibleRequestsAreRefused() {
        List<byte[]> leaves = leaves(4);
        assertThrows(IllegalArgumentException.class, () -> MerkleTree.inclusionProof(leaves, 4, 4));
        assertThrows(IllegalArgumentException.class, () -> MerkleTree.consistencyProof(leaves, 0, 4));
        assertThrows(IllegalArgumentException.class, () -> MerkleTree.consistencyProof(leaves, 3, 2));
        assertFalse(MerkleTree.verifyInclusion(MerkleTree.leafHash(leaves.get(0)), 5, 4, List.of(), MerkleTree.root(leaves, 4)));
        assertFalse(MerkleTree.verifyConsistency(4, MerkleTree.root(leaves, 4), 4, MerkleTree.root(leaves, 3), List.of()));
    }
}
