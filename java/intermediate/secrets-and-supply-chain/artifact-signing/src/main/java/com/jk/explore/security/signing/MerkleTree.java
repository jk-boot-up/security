package com.jk.explore.security.signing;

import java.util.ArrayList;
import java.util.List;

/**
 * The Merkle tree of a transparency log, as RFC 9162 (Certificate Transparency 2.0) defines it. A leaf is hashed as SHA-256(0x00 || data)
 * and a node as SHA-256(0x01 || left || right), so a leaf can never be passed off as a node. A tree of n leaves splits at the largest
 * power of two smaller than n.
 */
final class MerkleTree {

    private static final byte[] LEAF = {0x00};
    private static final byte[] NODE = {0x01};

    private MerkleTree() {
    }

    static byte[] leafHash(byte[] data) {
        return Digests.sha256(LEAF, data);
    }

    static byte[] nodeHash(byte[] left, byte[] right) {
        return Digests.sha256(NODE, left, right);
    }

    /** The root of the first n leaves. */
    static byte[] root(List<byte[]> leaves, int n) {
        return hash(leaves, 0, n);
    }

    private static byte[] hash(List<byte[]> leaves, int from, int to) {
        int n = to - from;
        if (n == 0) {
            return Digests.sha256();
        }
        if (n == 1) {
            return leafHash(leaves.get(from));
        }
        int k = split(n);
        return nodeHash(hash(leaves, from, from + k), hash(leaves, from + k, to));
    }

    /** The largest power of two smaller than n, for n of two or more. */
    static int split(int n) {
        return Integer.highestOneBit(n - 1);
    }

    /** The hashes needed to recompute the root of the first n leaves from leaf m. */
    static List<byte[]> inclusionProof(List<byte[]> leaves, int m, int n) {
        if (m < 0 || m >= n || n > leaves.size()) {
            throw new IllegalArgumentException("no such leaf");
        }
        List<byte[]> path = new ArrayList<>();
        inclusion(leaves, m, 0, n, path);
        return path;
    }

    private static void inclusion(List<byte[]> leaves, int m, int from, int to, List<byte[]> path) {
        int n = to - from;
        if (n == 1) {
            return;
        }
        int k = split(n);
        if (m < k) {
            inclusion(leaves, m, from, from + k, path);
            path.add(hash(leaves, from + k, to));
        } else {
            inclusion(leaves, m - k, from + k, to, path);
            path.add(hash(leaves, from, from + k));
        }
    }

    /** RFC 9162 section 2.1.3.2: does this path lead from the leaf at this index to this root? */
    static boolean verifyInclusion(byte[] leafHash, int index, int size, List<byte[]> path, byte[] root) {
        if (index < 0 || index >= size) {
            return false;
        }
        long fn = index;
        long sn = size - 1L;
        byte[] r = leafHash;
        for (byte[] p : path) {
            if (sn == 0) {
                return false;
            }
            if ((fn & 1) == 1 || fn == sn) {
                r = nodeHash(p, r);
                if ((fn & 1) == 0) {
                    while ((fn & 1) == 0 && fn != 0) {
                        fn >>= 1;
                        sn >>= 1;
                    }
                }
            } else {
                r = nodeHash(r, p);
            }
            fn >>= 1;
            sn >>= 1;
        }
        return sn == 0 && Digests.same(r, root);
    }

    /** The hashes that prove the tree of the first m leaves is a prefix of the tree of the first n. */
    static List<byte[]> consistencyProof(List<byte[]> leaves, int m, int n) {
        if (m < 1 || m > n || n > leaves.size()) {
            throw new IllegalArgumentException("no such pair of sizes");
        }
        List<byte[]> proof = new ArrayList<>();
        if (m < n) {
            subproof(leaves, m, 0, n, true, proof);
        }
        return proof;
    }

    private static void subproof(List<byte[]> leaves, int m, int from, int to, boolean complete, List<byte[]> proof) {
        int n = to - from;
        if (m == n) {
            if (!complete) {
                proof.add(hash(leaves, from, to));
            }
            return;
        }
        int k = split(n);
        if (m <= k) {
            subproof(leaves, m, from, from + k, complete, proof);
            proof.add(hash(leaves, from + k, to));
        } else {
            subproof(leaves, m - k, from + k, to, false, proof);
            proof.add(hash(leaves, from, from + k));
        }
    }

    /** RFC 9162 section 2.1.4.2: is the tree of size m with this root a prefix of the tree of size n with that root? */
    static boolean verifyConsistency(int m, byte[] firstRoot, int n, byte[] secondRoot, List<byte[]> proof) {
        if (m < 1 || m > n) {
            return false;
        }
        if (m == n) {
            return proof.isEmpty() && Digests.same(firstRoot, secondRoot);
        }
        List<byte[]> path = new ArrayList<>(proof);
        if (Integer.bitCount(m) == 1) {
            path.addFirst(firstRoot);
        }
        if (path.isEmpty()) {
            return false;
        }
        long fn = m - 1L;
        long sn = n - 1L;
        while ((fn & 1) == 1) {
            fn >>= 1;
            sn >>= 1;
        }
        byte[] fr = path.getFirst();
        byte[] sr = path.getFirst();
        for (byte[] c : path.subList(1, path.size())) {
            if (sn == 0) {
                return false;
            }
            if ((fn & 1) == 1 || fn == sn) {
                fr = nodeHash(c, fr);
                sr = nodeHash(c, sr);
                if ((fn & 1) == 0) {
                    while ((fn & 1) == 0 && fn != 0) {
                        fn >>= 1;
                        sn >>= 1;
                    }
                }
            } else {
                sr = nodeHash(sr, c);
            }
            fn >>= 1;
            sn >>= 1;
        }
        return sn == 0 && Digests.same(fr, firstRoot) && Digests.same(sr, secondRoot);
    }
}
