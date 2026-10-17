package com.jk.explore.security.validation;

import java.util.Locale;
import java.util.Set;

/** A shipping country as an ISO 3166 two-letter code, checked against the list the JDK ships. */
public record CountryCode(String value) {

    private static final Set<String> KNOWN = Set.copyOf(Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2));

    public static CountryCode parse(String raw) throws Rejected {
        String text = Text.bounded("country", raw, 2);
        if (!KNOWN.contains(text)) {
            throw new Rejected("country", "a two-letter country code such as IN or DE");
        }
        return new CountryCode(text);
    }
}
