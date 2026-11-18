package com.jk.explore.security.linuxhard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a {@code /proc/net/tcp} table, the kernel's list of TCP sockets. Each line holds the local address as hex
 * {@code address:port}, and the state; state {@code 0A} means the socket is listening.
 */
public final class PortTable {

    /** One listening socket: the hex text as the kernel wrote it, and what it means. */
    public record Listener(String local, String address, int port) {

        /** 0.0.0.0 means every interface: anyone who can reach the host can connect. */
        public boolean everywhere() {
            return address.equals("0.0.0.0");
        }
    }

    private PortTable() {
    }

    public static List<Listener> listening(Path table) throws IOException {
        List<Listener> out = new ArrayList<>();
        if (!Files.exists(table)) {
            return out;
        }
        for (String line : Files.readAllLines(table).stream().skip(1).toList()) {
            String[] p = line.trim().split("\\s+");
            if (p.length < 4 || !p[3].equals("0A")) {
                continue;
            }
            String[] local = p[1].split(":");
            out.add(new Listener(p[1], address(local[0]), Integer.parseInt(local[1], 16)));
        }
        return out;
    }

    /** The kernel writes an IPv4 address as one hex number in little-endian order: 0100007F is 127.0.0.1. */
    static String address(String hex) {
        long v = Long.parseLong(hex, 16);
        return (v & 0xFF) + "." + (v >> 8 & 0xFF) + "." + (v >> 16 & 0xFF) + "." + (v >> 24 & 0xFF);
    }
}
