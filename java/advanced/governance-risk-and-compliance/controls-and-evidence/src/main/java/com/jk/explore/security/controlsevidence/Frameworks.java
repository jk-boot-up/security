package com.jk.explore.security.controlsevidence;

import java.util.Map;

/** The names behind the identifiers: NIST CSF 2.0 functions and categories, and the ISO/IEC 27001:2022 Annex A themes and the controls the bank uses. */
public final class Frameworks {

    private Frameworks() {
    }

    /** A CSF 2.0 subcategory such as PR.DS-02: function PR, category DS, subcategory 02. */
    public record CsfId(String function, String category, String number) {

        public static CsfId parse(String id) {
            int dot = id.indexOf('.');
            int dash = id.indexOf('-');
            String function = id.substring(0, dot);
            String category = id.substring(dot + 1, dash);
            return new CsfId(function, category, id.substring(dash + 1));
        }
    }

    /** An Annex A control such as A.8.24: theme 8, control 24. */
    public record AnnexId(int theme, int number) {

        public static AnnexId parse(String id) {
            String[] parts = id.split("\\.");
            return new AnnexId(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
    }

    public static final Map<String, String> FUNCTIONS = Map.of(
            "GV", "Govern", "ID", "Identify", "PR", "Protect", "DE", "Detect", "RS", "Respond", "RC", "Recover");

    public static final Map<String, String> CATEGORIES = Map.ofEntries(
            Map.entry("GV.PO", "Policy"), Map.entry("GV.RM", "Risk Management Strategy"),
            Map.entry("ID.AM", "Asset Management"), Map.entry("ID.RA", "Risk Assessment"),
            Map.entry("PR.AA", "Identity, Authentication and Access"), Map.entry("PR.AT", "Awareness and Training"),
            Map.entry("PR.DS", "Data Security"), Map.entry("PR.PS", "Platform Security"),
            Map.entry("DE.CM", "Continuous Monitoring"), Map.entry("RS.MA", "Incident Management"),
            Map.entry("RC.RP", "Incident Recovery Plan"));

    public static final Map<Integer, String> THEMES = Map.of(5, "organizational", 6, "people", 7, "physical", 8, "technological");

    public static final Map<String, String> ANNEX_A = Map.ofEntries(
            Map.entry("A.5.1", "Policies for information security"), Map.entry("A.5.4", "Management responsibilities"),
            Map.entry("A.5.9", "Inventory of information and assets"), Map.entry("A.5.15", "Access control"),
            Map.entry("A.5.24", "Incident management planning"), Map.entry("A.5.30", "ICT readiness for business continuity"),
            Map.entry("A.6.3", "Security awareness and training"), Map.entry("A.8.8", "Management of technical vulnerabilities"),
            Map.entry("A.8.9", "Configuration management"), Map.entry("A.8.13", "Information backup"),
            Map.entry("A.8.16", "Monitoring activities"), Map.entry("A.8.20", "Networks security"),
            Map.entry("A.8.24", "Use of cryptography"), Map.entry("A.8.28", "Secure coding"));
}
