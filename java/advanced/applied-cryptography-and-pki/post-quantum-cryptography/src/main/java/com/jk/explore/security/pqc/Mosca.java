package com.jk.explore.security.pqc;

import java.util.List;

/**
 * Mosca's inequality: if the years data must stay secret (x) plus the years a migration takes (y) are more than the years until a
 * quantum computer that breaks the old algorithms exists (z), the data is already exposed to someone who records it today.
 * The inputs here are made-up illustrations for a bank; nobody knows z.
 */
final class Mosca {

    /** A kind of data and how long it must stay secret, and how long moving its protection takes. */
    record DataClass(String name, int secrecyYears, int migrationYears) {
        int needed() {
            return secrecyYears + migrationYears;
        }

        /** True when the data is exposed if the threat arrives in the given number of years. */
        boolean exposed(int yearsToThreat) {
            return needed() > yearsToThreat;
        }

        /** Years left before migration has to start, or a negative number when it is already late. */
        int startWithin(int yearsToThreat) {
            return yearsToThreat - needed();
        }
    }

    static final List<DataClass> BANK = List.of(
            new DataClass("card authorisation message", 1, 2),
            new DataClass("online banking session", 0, 2),
            new DataClass("customer statement archive", 10, 3),
            new DataClass("loan contract and identity documents", 30, 4),
            new DataClass("interbank settlement file", 7, 5));

    /** Three threat arrival times used as scenarios, not forecasts. */
    static final int[] SCENARIOS = {8, 15, 25};

    private Mosca() {
    }
}
