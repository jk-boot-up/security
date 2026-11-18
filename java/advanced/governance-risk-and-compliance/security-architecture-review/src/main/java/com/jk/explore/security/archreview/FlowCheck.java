package com.jk.explore.security.archreview;

import java.util.ArrayList;
import java.util.List;

/** A teaching class: the questions the flow rules ask about one flow, answered one by one, and the rules that fire. */
public record FlowCheck(Design.Flow flow) {

    public String zones() {
        return flow.from().zone() + " -> " + flow.to().zone();
    }

    public boolean crossesZone() {
        return !flow.from().zone().equals(flow.to().zone());
    }

    public boolean encrypted() {
        return flow.prop("encrypted").equals("yes");
    }

    /** How the caller is checked, or "none". */
    public String callerCheck() {
        String auth = flow.prop("auth");
        return auth.isEmpty() ? "none" : auth;
    }

    public boolean toConfidentialStore() {
        return flow.to().kind() == Design.Kind.DATASTORE && flow.to().prop("classification").equals("confidential");
    }

    public boolean fromOuterZone() {
        return flow.from().zone().equals("dmz") || flow.from().zone().equals("internet");
    }

    public List<String> rulesThatFire() {
        List<String> fire = new ArrayList<>();
        if (toConfidentialStore() && fromOuterZone()) {
            fire.add("no-direct-path-to-data");
        }
        if (crossesZone() && !encrypted()) {
            fire.add("cross-zone-encrypted");
        }
        if (crossesZone() && callerCheck().equals("none")) {
            fire.add("cross-zone-authenticated");
        }
        if (!encrypted() || callerCheck().equals("none")) {
            fire.add("every-flow-encrypted-and-authenticated");
        }
        return fire;
    }
}
