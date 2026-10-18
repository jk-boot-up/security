package com.jk.explore.security.signing;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * An append-only public log of release signatures. Entries are only ever added; the log signs each tree head (its size and root), and
 * hands out proofs that an entry is in the tree and that a later tree still contains an earlier one.
 */
final class TransparencyLog {

    /** A signed statement of the log's size and root hash. */
    record TreeHead(int size, byte[] root, byte[] signature) {

        byte[] signedBytes() {
            return ("tree-head/v1\nsize=" + size + "\nroot=" + Digests.hex(root) + "\n").getBytes(StandardCharsets.UTF_8);
        }
    }

    record Receipt(int index, int day) {
    }

    private final KeyPair key;
    private final List<byte[]> entries = new ArrayList<>();
    private final List<Integer> days = new ArrayList<>();

    TransparencyLog(KeyPair key) {
        this.key = key;
    }

    static TransparencyLog demo() {
        return new TransparencyLog(Ed25519.fromSeed(Digests.sha256("demo transparency log seed, not a real key".getBytes(StandardCharsets.UTF_8))));
    }

    /** A copy of this log as it stands, which can then grow differently: how a dishonest operator would show two views. */
    TransparencyLog fork() {
        TransparencyLog copy = new TransparencyLog(key);
        copy.entries.addAll(entries);
        copy.days.addAll(days);
        return copy;
    }

    PublicKey publicKey() {
        return key.getPublic();
    }

    /** Adds a signature, and records the day by the log's own clock, not the signer's. */
    Receipt append(SignedRelease release, int day) {
        entries.add(release.logEntry());
        days.add(day);
        return new Receipt(entries.size() - 1, day);
    }

    int size() {
        return entries.size();
    }

    byte[] entry(int index) {
        return entries.get(index);
    }

    int dayOf(int index) {
        return days.get(index);
    }

    TreeHead head() {
        byte[] root = MerkleTree.root(entries, entries.size());
        TreeHead unsigned = new TreeHead(entries.size(), root, new byte[0]);
        return new TreeHead(unsigned.size(), root, Ed25519.sign(key.getPrivate(), unsigned.signedBytes()));
    }

    List<byte[]> inclusionProof(int index, int size) {
        return MerkleTree.inclusionProof(entries, index, size);
    }

    List<byte[]> consistencyProof(int fromSize, int toSize) {
        return MerkleTree.consistencyProof(entries, fromSize, toSize);
    }

    static boolean headIsSigned(TreeHead head, PublicKey logKey) {
        return Ed25519.verify(logKey, head.signedBytes(), head.signature());
    }

    /** Is the earlier head a prefix of the later one, according to the log's proof? */
    static boolean consistent(TreeHead earlier, TreeHead later, List<byte[]> proof) {
        return MerkleTree.verifyConsistency(earlier.size(), earlier.root(), later.size(), later.root(), proof);
    }

    /** The index of this signature in the log, or -1. */
    int indexOf(SignedRelease release) {
        byte[] wanted = release.logEntry();
        for (int i = 0; i < entries.size(); i++) {
            if (java.util.Arrays.equals(entries.get(i), wanted)) {
                return i;
            }
        }
        return -1;
    }
}
