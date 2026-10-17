package com.jk.explore.security.backups;

import java.util.List;

/** The 3-2-1 rule as a checker: three copies of the data, on two kinds of media, one of them off site (and, today, one that cannot be changed). */
final class ThreeTwoOne {

    record Copy(String place, String medium, boolean offsite, boolean immutable) {
    }

    record Result(boolean threeCopies, boolean twoMedia, boolean oneOffsite, boolean oneImmutable) {
        int score() {
            return (threeCopies ? 1 : 0) + (twoMedia ? 1 : 0) + (oneOffsite ? 1 : 0) + (oneImmutable ? 1 : 0);
        }
    }

    private ThreeTwoOne() {
    }

    /** The first copy in the list is the live data itself. */
    static Result check(List<Copy> copies) {
        return new Result(copies.size() >= 3,
                copies.stream().map(Copy::medium).distinct().count() >= 2,
                copies.stream().anyMatch(Copy::offsite),
                copies.stream().skip(1).anyMatch(Copy::immutable));
    }
}
