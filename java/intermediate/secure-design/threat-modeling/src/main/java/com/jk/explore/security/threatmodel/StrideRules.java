package com.jk.explore.security.threatmodel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * STRIDE per element: simple rules that turn the model into a list of candidate threats. The rules do not decide anything; they make
 * sure nobody forgets to ask a question about an element or a flow. Each rule follows the shape of the diagram:
 * <ul>
 *   <li>spoofing: every external entity, and every process that receives a flow across a boundary;</li>
 *   <li>tampering: every flow that crosses a boundary;</li>
 *   <li>repudiation: every flow that moves money;</li>
 *   <li>information disclosure: every data store;</li>
 *   <li>denial of service: every public-facing process;</li>
 *   <li>elevation of privilege: every flow from a less trusted zone into a more trusted one.</li>
 * </ul>
 */
final class StrideRules {

    /** What each letter is asked of, in the words of the rules below. */
    static final Map<Stride, String> ASKED_OF = new EnumMap<>(Map.of(
            Stride.SPOOFING, "entities; processes fed across",
            Stride.TAMPERING, "flows that cross a boundary",
            Stride.REPUDIATION, "flows that move money",
            Stride.INFORMATION_DISCLOSURE, "data stores",
            Stride.DENIAL_OF_SERVICE, "public-facing processes",
            Stride.ELEVATION_OF_PRIVILEGE, "flows into a more trusted zone"));

    private StrideRules() {
    }

    /** The six questions asked of one flow: for each letter, whether it applies, and why. */
    static Map<Stride, String> explain(Model model, Flow f) {
        Map<Stride, String> why = new EnumMap<>(Stride.class);
        String a = model.zoneOf(f.from());
        String b = model.zoneOf(f.to());
        why.put(Stride.SPOOFING, "no   asked of entities and processes");
        why.put(Stride.TAMPERING, model.crosses(f) ? "yes  it crosses the " + model.boundaryOf(f).map(Boundary::name).orElse("an undrawn boundary")
                : "no   it stays in the " + a + " zone");
        why.put(Stride.REPUDIATION, f.money() ? "yes  it moves money" : "no   it moves no money");
        why.put(Stride.INFORMATION_DISCLOSURE, "no   asked of data stores");
        why.put(Stride.DENIAL_OF_SERVICE, "no   asked of public processes");
        why.put(Stride.ELEVATION_OF_PRIVILEGE, (model.raisesTrust(f) ? "yes  " : "no   ") + a + " (trust " + model.trust(a) + ") to " + b + " (trust " + model.trust(b) + ")");
        return why;
    }

    static List<Threat> generate(Model model) {
        List<Threat> threats = new ArrayList<>();
        for (Element e : model.elements()) {
            if (e.kind() == Kind.EXTERNAL_ENTITY) {
                threats.add(new Threat(Stride.SPOOFING, e.id(), "someone pretends to be the " + e.name()));
            }
            if (e.kind() == Kind.PROCESS && model.into(e.id()).stream().anyMatch(model::crosses)) {
                threats.add(new Threat(Stride.SPOOFING, e.id(), "a caller fakes its identity to the " + e.name()));
            }
            if (e.kind() == Kind.DATA_STORE) {
                threats.add(new Threat(Stride.INFORMATION_DISCLOSURE, e.id(), "the " + e.name() + " is read by the wrong person"));
            }
            if (e.kind() == Kind.PROCESS && e.publicFacing()) {
                threats.add(new Threat(Stride.DENIAL_OF_SERVICE, e.id(), "the " + e.name() + " is flooded until customers cannot pay"));
            }
        }
        for (Flow f : model.flows()) {
            String to = model.element(f.to()).name();
            if (model.crosses(f)) {
                threats.add(new Threat(Stride.TAMPERING, f.id(),
                        "the " + f.data() + " is changed in transit"));
            }
            if (f.money()) {
                threats.add(new Threat(Stride.REPUDIATION, f.id(),
                        "whoever sent the " + f.data() + " denies it"));
            }
            if (model.crosses(f) && model.raisesTrust(f)) {
                threats.add(new Threat(Stride.ELEVATION_OF_PRIVILEGE, f.id(), "a " + model.zoneOf(f.from()) + " caller gets the rights of the " + to));
            }
        }
        threats.sort(Comparator.comparing(Threat::stride).thenComparing(Threat::target));
        return threats;
    }
}
