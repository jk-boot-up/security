package com.jk.explore.security.threatmodel;

/** A trust boundary: the line between two zones where what is trusted on one side cannot be trusted on the other. */
record Boundary(String name, String zoneA, String zoneB) {

    boolean separates(String a, String b) {
        return zoneA.equals(a) && zoneB.equals(b) || zoneA.equals(b) && zoneB.equals(a);
    }
}
