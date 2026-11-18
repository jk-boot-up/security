package com.jk.explore.security.privacyeng;

import java.time.LocalDate;

/** A teaching class: the coarse values that Anonymity.COARSE computes in SQL, worked out for one customer in Java. */
public final class Coarse {

    private Coarse() {
    }

    /** The first three digits of the postcode. */
    public static String postcode(String postcode) {
        return postcode.substring(0, 3);
    }

    /** The decade of birth, such as 1970s. */
    public static String decade(LocalDate birthDate) {
        return (birthDate.getYear() / 10 * 10) + "s";
    }
}
