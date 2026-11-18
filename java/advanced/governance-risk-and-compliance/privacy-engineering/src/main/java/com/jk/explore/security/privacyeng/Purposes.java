package com.jk.explore.security.privacyeng;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Why the bank holds each personal column of the customers table: the purposes and the columns each one needs. */
public final class Purposes {

    public static final List<String> PERSONAL_COLUMNS = List.of("name", "email", "phone", "postcode", "birth_date", "gender", "national_id", "marketing_consent", "consent_date");

    /** A purpose, the short code the demo prints for it, and the columns it needs. */
    public record Purpose(String name, String code, Set<String> needs) {
    }

    public static final List<Purpose> ALL = List.of(
            new Purpose("payments", "pay", Set.of("name", "postcode")),
            new Purpose("statements", "stmt", Set.of("name", "email", "postcode")),
            new Purpose("marketing", "mkt", Set.of("email", "marketing_consent", "consent_date")),
            new Purpose("fraud analytics", "fraud", Set.of("postcode", "birth_date")),
            new Purpose("identity checks required by law", "id", Set.of("name", "birth_date", "national_id")));

    private Purposes() {
    }

    public static Set<String> needed() {
        Set<String> all = new TreeSet<>();
        ALL.forEach(p -> all.addAll(p.needs()));
        return all;
    }

    /** The personal columns that no purpose needs: collected, stored, protected and never used. */
    public static List<String> withoutPurpose() {
        return PERSONAL_COLUMNS.stream().filter(c -> !needed().contains(c)).toList();
    }
}
