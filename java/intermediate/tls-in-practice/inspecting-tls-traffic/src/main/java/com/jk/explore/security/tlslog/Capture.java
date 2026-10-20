package com.jk.explore.security.tlslog;

import java.util.List;

/**
 * What a network observer (or a packet capture file) holds for one connection: the hello values that are sent in the clear, and the encrypted records.
 * {@code clientKeyShare} is the client's public value for ECDHE, or the premaster secret encrypted with the server's long-term key for static RSA.
 */
public record Capture(String label, boolean ecdhe, byte[] clientRandom, byte[] serverRandom, byte[] clientKeyShare, byte[] serverKeyShare, List<Packet> packets) {
}
