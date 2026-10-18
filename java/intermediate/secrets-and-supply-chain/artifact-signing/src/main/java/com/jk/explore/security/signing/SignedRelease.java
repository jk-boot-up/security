package com.jk.explore.security.signing;

/** A detached signature: the statement's bytes, the id of the key that signed them, and the Ed25519 signature. */
record SignedRelease(byte[] statement, String keyId, byte[] signature) {

    /** The bytes a transparency log records for this signature. */
    byte[] logEntry() {
        byte[] id = (keyId + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] out = new byte[id.length + statement.length + signature.length];
        System.arraycopy(id, 0, out, 0, id.length);
        System.arraycopy(statement, 0, out, id.length, statement.length);
        System.arraycopy(signature, 0, out, id.length + statement.length, signature.length);
        return out;
    }
}
