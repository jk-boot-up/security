package com.jk.explore.security.patching;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Matches an inventory against the advisories. */
final class Audit {

    private Audit() {
    }

    /** Every advisory that applies to an installed component, in the order of the inventory. */
    static List<Finding> scan(Sbom sbom, List<Advisory> advisories) {
        List<Finding> findings = new ArrayList<>();
        for (Component component : sbom.components()) {
            if (component.ref().equals(sbom.root())) {
                continue;
            }
            SemVer installed = SemVer.parse(component.version());
            for (Advisory advisory : advisories) {
                if (advisory.component().equals(component.name()) && advisory.affected().contains(installed)) {
                    double score = Cvss31.base(advisory.vector());
                    List<String> path = sbom.pathTo(component.ref()).orElse(List.of());
                    if (path.isEmpty()) {
                        continue;
                    }
                    findings.add(new Finding(advisory, component.ref(), path, score, Cvss31.severity(score),
                            sbom.usedFeatures().contains(advisory.feature())));
                }
            }
        }
        return findings;
    }

    /** Reachable problems first, then the higher score, then the id. */
    static List<Finding> triage(List<Finding> findings) {
        return findings.stream()
                .sorted(Comparator.comparing((Finding f) -> !f.reachable())
                        .thenComparing(Comparator.comparingDouble(Finding::score).reversed())
                        .thenComparing(f -> f.advisory().id()))
                .toList();
    }
}
