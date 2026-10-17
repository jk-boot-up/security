package com.jk.explore.security.threats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** The list of risks, ranked from the worst to the least. */
public class RiskRegister {

    private final List<Risk> risks;

    public RiskRegister(List<Risk> risks) {
        this.risks = List.copyOf(risks);
    }

    /** Highest score first; a tie goes to the higher impact, then to the lower id, so the order never varies. */
    public List<Risk> ranked() {
        return risks.stream()
                .sorted(Comparator.comparingInt(Risk::score).reversed()
                        .thenComparing(Comparator.comparingInt(Risk::impact).reversed())
                        .thenComparing(Risk::id))
                .toList();
    }

    public int totalScore() {
        return risks.stream().mapToInt(Risk::score).sum();
    }

    public Risk byId(String id) {
        return risks.stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }

    /** A copy of the register in which the named risks are scored again, as a different reviewer might. */
    public RiskRegister rescored(Map<String, int[]> likelihoodAndImpactById) {
        List<Risk> out = new ArrayList<>();
        for (Risk r : risks) {
            int[] s = likelihoodAndImpactById.get(r.id());
            out.add(s == null ? r : r.rescored(s[0], s[1]));
        }
        return new RiskRegister(out);
    }

    public List<Risk> all() {
        return risks;
    }
}
