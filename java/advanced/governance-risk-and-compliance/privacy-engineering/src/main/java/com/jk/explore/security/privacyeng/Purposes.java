package com.jk.explore.security.privacyeng;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Why the bank holds each personal column of the customers table: the purposes and the columns each one needs. */
public final class Purposes {

    public static final List<String> PERSONAL_COLUMNS = List.of("name", "email", "phone", "postcode", "birth_date", "gender", "national_id", "marketing_consent", "consent_date");

    public static final Map<String, Set<String>> NEEDS = Map.of(
            "payments", Set.of("name", "postcode"),
            "statements", Set.of("name", "email", "postcode"),
            "marketing", Set.of("email", "marketing_consent", "consent_date"),
            "fraud analytics", Set.of("postcode", "birth_date"),
            "identity checks required by law", Set.of("name", "birth_date", "national_id"));

    private Purposes() {
    }

    public static Set<String> needed() {
        Set<String> all = new TreeSet<>();
        NEEDS.values().forEach(all::addAll);
        return all;
    }

    /** The personal columns that no purpose needs: collected, stored, protected and never used. */
    public static List<String> withoutPurpose() {
        return PERSONAL_COLUMNS.stream().filter(c -> !needed().contains(c)).toList();
    }
}
