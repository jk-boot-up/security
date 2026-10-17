package com.jk.explore.security.threatmodel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    private StrideRules() {
    }

    static List<Threat> generate(Model model) {
        List<Threat> threats = new ArrayList<>();
        for (Element e : model.elements()) {
            if (e.kind() == Kind.EXTERNAL_ENTITY) {
                threats.add(new Threat(Stride.SPOOFING, e.id(), "someone pretends to be the " + e.name()));
            }
            if (e.kind() == Kind.PROCESS && model.into(e.id()).stream().anyMatch(model::crosses)) {
                threats.add(new Threat(Stride.SPOOFING, e.id(), "a caller across a boundary pretends to be someone else to the " + e.name()));
            }
            if (e.kind() == Kind.DATA_STORE) {
                threats.add(new Threat(Stride.INFORMATION_DISCLOSURE, e.id(), "the " + e.name() + " is read by someone who should not see it"));
            }
            if (e.kind() == Kind.PROCESS && e.publicFacing()) {
                threats.add(new Threat(Stride.DENIAL_OF_SERVICE, e.id(), "the " + e.name() + " is flooded until customers cannot pay"));
            }
        }
        for (Flow f : model.flows()) {
            String from = model.element(f.from()).name();
            String to = model.element(f.to()).name();
            if (model.crosses(f)) {
                String boundary = model.boundaryOf(f).map(Boundary::name).orElse("an undrawn boundary");
                threats.add(new Threat(Stride.TAMPERING, f.id(), "the " + f.data() + " is changed between the " + from + " and the " + to + ", across the " + boundary));
            }
            if (f.money()) {
                threats.add(new Threat(Stride.REPUDIATION, f.id(), "whoever sent the " + f.data() + " through the " + from + " later denies sending it"));
            }
            if (model.crosses(f) && model.raisesTrust(f)) {
                threats.add(new Threat(Stride.ELEVATION_OF_PRIVILEGE, f.id(), "a caller from the " + model.zoneOf(f.from()) + " zone gains the rights of the " + to));
            }
        }
        threats.sort(Comparator.comparing(Threat::stride).thenComparing(Threat::target));
        return threats;
    }
}
