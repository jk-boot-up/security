package com.jk.explore.security.threats;

import java.util.ArrayList;
import java.util.List;

/** The attack surface: the list of every entry point an attacker could try. */
public class Surface {

    private final List<EntryPoint> entryPoints = new ArrayList<>();

    public Surface add(EntryPoint entryPoint) {
        entryPoints.add(entryPoint);
        return this;
    }

    public int size() {
        return entryPoints.size();
    }

    public long count(Exposure exposure) {
        return entryPoints.stream().filter(e -> e.exposure() == exposure).count();
    }

    public List<EntryPoint> entryPoints() {
        return List.copyOf(entryPoints);
    }

    /** One line such as "8 entry points: 3 public, 3 authenticated, 2 internal". */
    public String summary() {
        return size() + " entry points: " + count(Exposure.PUBLIC) + " public, "
                + count(Exposure.AUTHENTICATED) + " authenticated, " + count(Exposure.INTERNAL) + " internal";
    }
}
