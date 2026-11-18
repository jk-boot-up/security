package com.jk.explore.security.archreview;

import java.util.List;

/** The STRIDE letters a review asks about each kind of part and about each flow, and how many questions that makes. */
public final class Stride {

    private Stride() {
    }

    /** Spoofing, Tampering, Repudiation, Information disclosure, Denial of service, Elevation of privilege. */
    public static List<String> letters(Design.Kind kind) {
        return switch (kind) {
            case PERSON, EXTERNAL -> List.of("S", "R");
            case PROCESS -> List.of("S", "T", "R", "I", "D", "E");
            case DATASTORE -> List.of("T", "R", "I", "D");
        };
    }

    /** A flow can be tampered with, read or stopped. */
    public static final List<String> FLOW = List.of("T", "I", "D");

    public static int questions(Design design) {
        int n = 0;
        for (Design.Part p : design.parts()) {
            n += letters(p.kind()).size();
        }
        return n + FLOW.size() * design.flows().size();
    }
}
