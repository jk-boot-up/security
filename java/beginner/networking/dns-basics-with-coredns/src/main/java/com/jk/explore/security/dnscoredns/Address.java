package com.jk.explore.security.dnscoredns;

/** IPv4 addresses as four bytes packed into an int. */
final class Address {

    private Address() {
    }

    static int parse(String text) {
        String[] parts = text.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("not an IPv4 address: " + text);
        }
        int value = 0;
        for (String part : parts) {
            int octet = Integer.parseInt(part);
            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("not an IPv4 address: " + text);
            }
            value = (value << 8) | octet;
        }
        return value;
    }

    static String text(int address) {
        return (address >>> 24) + "." + ((address >> 16) & 0xff) + "." + ((address >> 8) & 0xff) + "." + (address & 0xff);
    }
}
