package com.jk.explore.security.privacyeng;

import java.util.Map;
import java.util.TreeSet;

public class PrivacyEngineeringDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String pct(double x) {
        return String.format("%.1f%%", x * 100);
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        try (Pg pg = new Pg()) {
            Seed.create(pg);

            out.append("--- what is collected, and why\n");
            out.append("customers: " + pg.count("SELECT count(*) FROM customers") + ", accounts: " + pg.count("SELECT count(*) FROM accounts") + ", transactions: " + pg.count("SELECT count(*) FROM transactions") + ", support notes: " + pg.count("SELECT count(*) FROM support_notes") + "\n");
            out.append("personal columns in the customers table: " + Purposes.PERSONAL_COLUMNS.size() + ", needed by at least one of the " + Purposes.NEEDS.size() + " purposes: " + Purposes.needed().size() + "\n");
            out.append("collected for no purpose: " + Purposes.withoutPurpose() + "\n");

            out.append("--- who can be picked out of data that has no names\n");
            out.append("rows alone in their group on postcode, birth date and gender: " + pct(Anonymity.shareBelow(pg, "customers", Anonymity.FULL, 2)) + ", in a group of fewer than 5: " + pct(Anonymity.shareBelow(pg, "customers", Anonymity.FULL, 5)) + "\n");
            out.append("with the postcode cut to 3 digits and the birth date to its decade: alone " + pct(Anonymity.shareBelow(pg, "customers", Anonymity.COARSE, 2)) + ", in a group of fewer than 5: " + pct(Anonymity.shareBelow(pg, "customers", Anonymity.COARSE, 5)) + "\n");
            out.append("smallest group: " + Anonymity.smallest(pg, "customers", Anonymity.FULL) + " before, " + Anonymity.smallest(pg, "customers", Anonymity.COARSE) + " after\n");
            out.append("an outside list names 70% of the customers with those three values: named from the published rows: " + Anonymity.namedFromOutside(pg, 7) + " with the full values, " + Anonymity.namedFromOutsideCoarse(pg, 7) + " with the coarse ones\n");

            out.append("--- pseudonyms for analytics\n");
            Pseudonyms pseudonyms = new Pseudonyms("analytics-key-one");
            int rows = pseudonyms.publish(pg);
            out.append("rows published for analytics: " + rows + ", distinct pseudonyms: " + pg.count("SELECT count(DISTINCT pseudonym) FROM analytics_tx") + "\n");
            out.append("the same customer under the same key gets the same pseudonym: " + (pseudonyms.of(17).equals(new Pseudonyms("analytics-key-one").of(17)) ? "yes" : "no") + ", under another key a different one: " + (!pseudonyms.of(17).equals(new Pseudonyms("analytics-key-two").of(17)) ? "yes" : "no") + "\n");
            out.append("the pseudonym is a name replaced, not a person hidden: the rows still carry postcode, birth date and gender\n");

            out.append("--- retention on " + Seed.TODAY + "\n");
            Retention.Plan plan = Retention.run(pg, Seed.TODAY);
            out.append("transactions older than " + Retention.TRANSACTION_YEARS + " years: " + plan.expiredTransactions() + ", kept for a legal hold: " + plan.underHold() + ", deleted: " + (plan.expiredTransactions() - plan.underHold()) + "\n");
            out.append("marketing consents older than " + Retention.CONSENT_YEARS + " years withdrawn: " + plan.expiredConsents() + "\n");
            out.append("support notes older than " + Retention.NOTE_YEARS + " year deleted: " + plan.expiredNotes() + "\n");
            out.append("transactions left: " + pg.count("SELECT count(*) FROM transactions") + ", older than the limit: " + pg.count("SELECT count(*) FROM transactions WHERE booked_on < ?", java.sql.Date.valueOf(Seed.TODAY.minusYears(Retention.TRANSACTION_YEARS))) + "\n");

            out.append("analytics rows from before the limit, which the job does not touch: " + pg.count("SELECT count(*) FROM analytics_tx WHERE booked_year < ?", Seed.TODAY.minusYears(Retention.TRANSACTION_YEARS).getYear()) + " of " + pg.count("SELECT count(*) FROM analytics_tx") + "\n");

            out.append("--- a customer asks for their data and then for its erasure\n");
            out.append("tables that hold personal data, found from the schema: " + Subject.dataMap(pg) + "\n");
            Subject subject = new Subject(pg, 17);
            Map<String, Long> export = subject.export();
            out.append("records about the customer: " + export + ", analytics rows: " + subject.analyticsRows(pseudonyms) + "\n");
            java.util.List<String> ids = subject.erase(pseudonyms);
            out.append("erased: personal columns blanked, notes and analytics rows deleted; kept by law: " + export.get("transactions") + " transactions\n");
            Map<String, Long> remnants = Subject.remnants(pg, ids);
            out.append("rows that still mention the customer's name, email, phone or national id: " + new TreeSet<>(remnants.keySet()).stream().map(t -> t + " " + remnants.get(t)).toList() + "\n");
            long before = remnants.values().stream().mapToLong(Long::longValue).sum();
            Subject.scrub(pg, ids);
            out.append("rows with a mention before the scrub: " + before + ", after: " + Subject.remnants(pg, ids).values().stream().mapToLong(Long::longValue).sum() + "\n");
        }
        out.append("--- the bill\n");
        out.append("removing the name does not make data anonymous: the other columns find the person\n");
        out.append("personal data is copied into notes, analytics and other people's records, and erasure has to look there too\n");
        out.append("the law can require a bank to keep what a customer asks it to erase: say so, and keep only that\n");
        return out.toString();
    }
}
