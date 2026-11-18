package com.jk.explore.security.containerhard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A teaching class: reads the {@code CapEff} line of {@code /proc/self/status}, a hexadecimal bit mask in which bit n is the capability with number n
 * (from linux/capability.h). The sniff-network probe tests bit 13 the same way.
 */
public final class Capabilities {

    /** The capability numbers this lesson names: Docker's default fourteen, and SYS_ADMIN. */
    public static final Map<Integer, String> NAMES = new TreeMap<>(Map.ofEntries(
            Map.entry(0, "CHOWN"), Map.entry(1, "DAC_OVERRIDE"), Map.entry(3, "FOWNER"), Map.entry(4, "FSETID"), Map.entry(5, "KILL"),
            Map.entry(6, "SETGID"), Map.entry(7, "SETUID"), Map.entry(8, "SETPCAP"), Map.entry(10, "NET_BIND_SERVICE"), Map.entry(13, "NET_RAW"),
            Map.entry(18, "SYS_CHROOT"), Map.entry(21, "SYS_ADMIN"), Map.entry(27, "MKNOD"), Map.entry(29, "AUDIT_WRITE"), Map.entry(31, "SETFCAP")));

    public static final int NET_RAW = 13;
    public static final int SYS_ADMIN = 21;

    private Capabilities() {
    }

    /** Whether bit {@code n} of the mask is set: {@code (mask >> n) & 1}. */
    public static boolean held(String capEff, int n) {
        return (Long.parseUnsignedLong(capEff, 16) >> n & 1) == 1;
    }

    /** The numbers of the set bits, lowest first. */
    public static List<Integer> bits(String capEff) {
        long mask = Long.parseUnsignedLong(capEff, 16);
        List<Integer> out = new ArrayList<>();
        for (int n = 0; n < 64; n++) {
            if ((mask >> n & 1) == 1) {
                out.add(n);
            }
        }
        return out;
    }

    /** The names of the set bits; a bit this lesson does not name is written as its number. */
    public static List<String> names(String capEff) {
        return bits(capEff).stream().map(n -> NAMES.getOrDefault(n, "#" + n)).toList();
    }
}
