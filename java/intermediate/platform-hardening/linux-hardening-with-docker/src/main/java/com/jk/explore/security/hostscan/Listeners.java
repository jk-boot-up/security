package com.jk.explore.security.hostscan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reads the kernel's socket tables, /proc/net/tcp and /proc/net/tcp6. Each line holds the local address as hex
 * {@code address:port} and the state; state {@code 0A} means listening.
 */
final class Listeners {

    /** One listening socket, as the kernel wrote it. */
    record Listener(String hexAddress, String hexPort, int port) {

        /** An all-zero address, 0.0.0.0 or ::, means every interface: anyone who can reach the container can connect. */
        boolean everywhere() {
            return hexAddress.matches("0+");
        }

        /** The address in the usual notation: an IPv4 address is one little-endian hex number, 0100007F is 127.0.0.1. */
        String address() {
            if (hexAddress.length() != 8) {
                return everywhere() ? "::" : "an IPv6 address";
            }
            long v = Long.parseLong(hexAddress, 16);
            return (v & 0xFF) + "." + (v >> 8 & 0xFF) + "." + (v >> 16 & 0xFF) + "." + (v >> 24 & 0xFF);
        }
    }

    private Listeners() {
    }

    /** The listening sockets in the lines of both tables, sorted by port. */
    static List<Listener> read(List<String> lines) {
        List<Listener> out = new ArrayList<>();
        for (String line : lines) {
            String[] f = line.strip().split("\\s+");
            if (f.length < 4 || !f[3].equals("0A")) {
                continue;
            }
            String[] local = f[1].split(":");
            out.add(new Listener(local[0], local[1], Integer.parseInt(local[1], 16)));
        }
        out.sort(Comparator.comparingInt(Listener::port));
        return out;
    }
}
