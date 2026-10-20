package com.jk.explore.security.tlslog;

/** One thing seen on the wire: who sent it, and the bytes. */
public record Packet(boolean fromClient, byte[] bytes) {
}
