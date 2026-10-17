package com.jk.explore.security.threatmodel;

import java.util.Set;

/**
 * One box in the data-flow diagram: who or what it is, which zone it sits in, who owns it, and which controls it has.
 * {@code owner} is null when nobody has been named, which the completeness check reports for data stores.
 */
record Element(String id, String name, Kind kind, String zone, String owner, boolean publicFacing, Set<String> controls) {

    Element {
        controls = Set.copyOf(controls);
    }

    boolean has(String control) {
        return controls.contains(control);
    }
}
