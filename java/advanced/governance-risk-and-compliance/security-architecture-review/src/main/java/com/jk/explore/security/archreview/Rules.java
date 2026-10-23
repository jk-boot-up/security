package com.jk.explore.security.archreview;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The review rules, each a few lines of Java over the design: trust zones, flows, data stores, owners and decisions. */
public final class Rules {

    private Rules() {
    }

    public static final List<String> FIRST_SET = List.of("no-direct-path-to-data", "cross-zone-encrypted", "cross-zone-authenticated", "wide-access-to-data",
            "confidential-encrypted-at-rest", "has-owner", "decision-is-accepted", "confidential-has-decision");
    public static final List<String> ALL = List.of("no-direct-path-to-data", "cross-zone-encrypted", "cross-zone-authenticated", "wide-access-to-data",
            "confidential-encrypted-at-rest", "has-owner", "decision-is-accepted", "confidential-has-decision", "every-flow-encrypted-and-authenticated");

    private static boolean weak(String auth) {
        return auth.isEmpty() || auth.equals("none");
    }

    public static List<Finding> run(Design design, Map<String, Adr> adrs, List<String> rules) {
        List<Finding> out = new ArrayList<>();
        for (Design.Flow f : design.flows()) {
            if (rules.contains("no-direct-path-to-data") && f.to().kind() == Design.Kind.DATASTORE && f.to().prop("classification").equals("confidential") && (f.from().zone().equals("dmz") || f.from().zone().equals("internet"))) {
                out.add(new Finding("no-direct-path-to-data", f.id(), f.from().name() + " in the " + f.from().zone() + " zone reaches confidential data directly"));
            }
            if (rules.contains("cross-zone-encrypted") && f.crossesZones() && !f.prop("encrypted").equals("yes")) {
                out.add(new Finding("cross-zone-encrypted", f.id(), "a flow between zones is not encrypted"));
            }
            if (rules.contains("cross-zone-authenticated") && f.crossesZones() && weak(f.prop("auth"))) {
                out.add(new Finding("cross-zone-authenticated", f.id(), "a flow between zones does not authenticate the caller"));
            }
            if (rules.contains("every-flow-encrypted-and-authenticated") && (!f.prop("encrypted").equals("yes") || weak(f.prop("auth")))) {
                out.add(new Finding("every-flow-encrypted-and-authenticated", f.id(), "a flow is not encrypted or not authenticated"));
            }
        }
        for (Design.Part p : design.parts()) {
            if (rules.contains("wide-access-to-data") && p.kind() == Design.Kind.DATASTORE) {
                long callers = design.flows().stream().filter(f -> f.to().equals(p) && f.from().kind() == Design.Kind.PROCESS).count();
                if (callers >= 3) {
                    out.add(new Finding("wide-access-to-data", p.id(), callers + " containers reach this data store"));
                }
            }
            if (rules.contains("confidential-encrypted-at-rest") && p.kind() == Design.Kind.DATASTORE && p.prop("classification").equals("confidential") && !p.prop("encrypted-at-rest").equals("yes")) {
                out.add(new Finding("confidential-encrypted-at-rest", p.id(), "confidential data is not encrypted at rest"));
            }
            if (rules.contains("has-owner") && p.prop("owner").isEmpty()) {
                out.add(new Finding("has-owner", p.id(), "nobody owns this part"));
            }
            if (rules.contains("decision-is-accepted") && !p.prop("adr").isEmpty()) {
                Adr adr = adrs.get(p.prop("adr"));
                if (adr == null || !adr.accepted()) {
                    out.add(new Finding("decision-is-accepted", p.id(), "rests on decision " + p.prop("adr") + (adr == null ? ", which does not exist" : ", which is " + adr.status())));
                }
            }
            if (rules.contains("confidential-has-decision") && p.kind() == Design.Kind.DATASTORE && p.prop("classification").equals("confidential") && p.prop("adr").isEmpty()) {
                out.add(new Finding("confidential-has-decision", p.id(), "a confidential data store has no recorded decision"));
            }
        }
        out.sort(java.util.Comparator.comparing(Finding::rule).thenComparing(Finding::subject));
        return out;
    }

    /** The STRIDE questions a review asks about the design, one set for each part and flow by its kind. */
    public static int strideQuestions(Design design) {
        int n = 0;
        for (Design.Part p : design.parts()) {
            n += switch (p.kind()) {
                case PERSON, EXTERNAL -> 2;
                case PROCESS -> 6;
                case DATASTORE -> 4;
            };
        }
        return n + 3 * design.flows().size();
    }
}
