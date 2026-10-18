package com.jk.explore.security.passkeys;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * The authenticator data that the authenticator itself writes: the SHA-256 hash of the relying party id it signed for, flags for user
 * presence and user verification, and a signature counter. At registration it also carries the new credential's id. 37 bytes when signing in.
 */
record AuthenticatorData(byte[] rpIdHash, int flags, long signCount, byte[] credentialId) {

    static final int USER_PRESENT = 0x01;
    static final int USER_VERIFIED = 0x04;
    static final int ATTESTED_CREDENTIAL = 0x40;

    boolean has(int flag) {
        return (flags & flag) != 0;
    }

    /** rpIdHash (32) | flags (1) | signCount (4, big-endian) | [aaguid (16) | credentialId length (2) | credentialId]. */
    byte[] encode() {
        int extra = credentialId == null ? 0 : 16 + 2 + credentialId.length;
        ByteBuffer out = ByteBuffer.allocate(37 + extra);
        out.put(rpIdHash).put((byte) flags).putInt((int) signCount);
        if (credentialId != null) {
            out.put(new byte[16]).putShort((short) credentialId.length).put(credentialId);
        }
        return out.array();
    }

    static AuthenticatorData decode(byte[] bytes) {
        if (bytes.length < 37) {
            throw new IllegalArgumentException("authenticator data shorter than 37 bytes");
        }
        ByteBuffer in = ByteBuffer.wrap(bytes);
        byte[] hash = new byte[32];
        in.get(hash);
        int flags = in.get() & 0xff;
        long count = in.getInt() & 0xffffffffL;
        byte[] id = null;
        if ((flags & ATTESTED_CREDENTIAL) != 0) {
            if (in.remaining() < 18) {
                throw new IllegalArgumentException("attested credential data cut short");
            }
            in.position(in.position() + 16);
            int length = in.getShort() & 0xffff;
            if (in.remaining() != length) {
                throw new IllegalArgumentException("credential id length does not match");
            }
            id = new byte[length];
            in.get(id);
        } else if (in.remaining() != 0) {
            throw new IllegalArgumentException("unexpected bytes after the authenticator data");
        }
        return new AuthenticatorData(hash, flags, count, id);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AuthenticatorData a && Arrays.equals(rpIdHash, a.rpIdHash) && flags == a.flags
                && signCount == a.signCount && Arrays.equals(credentialId, a.credentialId);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(rpIdHash) * 31 + flags;
    }
}
