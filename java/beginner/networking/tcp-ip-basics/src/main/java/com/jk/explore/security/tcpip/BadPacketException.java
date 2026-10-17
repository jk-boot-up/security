package com.jk.explore.security.tcpip;

/** A packet or segment that is too short, malformed or fails its checksum. */
final class BadPacketException extends Exception {

    BadPacketException(String message) {
        super(message);
    }
}
