package com.jk.explore.security.threatmodel;

import java.util.Map;

/**
 * Likelihood and impact on a scale of one to five, and risk as their product. Likelihood follows the least trusted zone the threat touches,
 * because that is where an attacker starts. Impact follows what is at stake: money first, then the parts that hold or move it.
 */
record Rating(int likelihood, int impact) {

    private static final Map<String, Integer> LIKELIHOOD_BY_ZONE = Map.of(
            BankModel.DEVICE, 5, BankModel.DMZ, 4, BankModel.PARTNER, 3, BankModel.INTERNAL, 2);
    private static final Map<String, Integer> IMPACT_BY_ELEMENT = Map.of(
            "S1", 5, "P3", 5, "P2", 4, "E1", 4, "E2", 4, "S2", 3, "P1", 3);

    int risk() {
        return likelihood * impact;
    }

    static Rating of(Model model, Threat threat) {
        if (model.has(threat.target())) {
            Element e = model.element(threat.target());
            return new Rating(LIKELIHOOD_BY_ZONE.get(e.zone()), IMPACT_BY_ELEMENT.getOrDefault(e.id(), 3));
        }
        Flow f = model.flows().stream().filter(x -> x.id().equals(threat.target())).findFirst().orElseThrow();
        String from = model.zoneOf(f.from());
        String to = model.zoneOf(f.to());
        String weaker = model.trust(from) <= model.trust(to) ? from : to;
        return new Rating(LIKELIHOOD_BY_ZONE.get(weaker), f.money() ? 5 : 3);
    }
}
