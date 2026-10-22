package com.jk.explore.security.pytmmodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * What the team says about each element and flow: pytm asks about encryption, authentication, access control, validation and resilience, and its rules produce a finding for each
 * threat of its library whose condition the answers do not rule out. The stages add answers in the order a design review would.
 */
public final class Answers {

    /** The stages, in order; each includes the ones before it. */
    public enum Stage {
        NONE("no answers: every default is the worst case"), ENCRYPTION("traffic and stores are encrypted"), AUTHENTICATION("callers and services are authenticated"),
        ACCESS_AND_INPUT("access control, least privilege and input handling"), RESILIENCE("hardening, limits and logging");

        final String text;

        Stage(String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }
    }

    private Answers() {
    }

    private static final Set<String> TLS_FLOWS = Set.of("f2", "f3", "f4", "f5", "f6", "f7", "f8");
    private static final Set<String> AUTH_FLOWS = Set.of("f2", "f3", "f6", "f7");
    private static final Set<String> SERVICES = Set.of("gateway", "payments");
    private static final Set<String> STORES = Set.of("accounts", "audit", "vault");

    /** The Python lines that set the answers of one element (a variable name) at a stage. */
    public static List<String> forElement(Design.Element e, Stage stage) {
        List<String> lines = new ArrayList<>();
        int s = stage.ordinal();
        if (s >= Stage.ENCRYPTION.ordinal() && STORES.contains(e.id())) {
            lines.add(e.id() + ".controls.isEncrypted = True");
        }
        if (s >= Stage.AUTHENTICATION.ordinal() && SERVICES.contains(e.id())) {
            lines.add(e.id() + ".controls.implementsAuthenticationScheme = True");
            lines.add(e.id() + ".controls.authenticationScheme = \"OAuth 2.0 bearer tokens\"");
            lines.add(e.id() + ".controls.usesStrongSessionIdentifiers = True");
        }
        if (s >= Stage.ACCESS_AND_INPUT.ordinal()) {
            if (SERVICES.contains(e.id())) {
                for (String p : List.of("hasAccessControl", "implementsPOLP", "validatesInput", "sanitizesInput", "encodesOutput", "implementsServerSideValidation", "usesParameterizedInput", "checksInputBounds",
                        "validatesHeaders", "validatesContentType", "implementsStrictHTTPValidation")) {
                    lines.add(e.id() + ".controls." + p + " = True");
                }
            }
            if (STORES.contains(e.id())) {
                lines.add(e.id() + ".controls.hasAccessControl = True");
                lines.add(e.id() + ".controls.implementsPOLP = True");
            }
        }
        if (s >= Stage.RESILIENCE.ordinal() && (SERVICES.contains(e.id()) || STORES.contains(e.id()) || e.id().equals("app"))) {
            for (String p : List.of("isHardened", "isResilient", "handlesResourceConsumption", "handlesCrashes", "handlesInterruptions", "definesConnectionTimeout", "tracksExecutionFlow", "usesSecureFunctions")) {
                lines.add(e.id() + ".controls." + p + " = True");
            }
        }
        return lines;
    }

    /** The Python lines that set the answers of one flow at a stage. */
    public static List<String> forFlow(Design.Flow f, Stage stage) {
        List<String> lines = new ArrayList<>();
        int s = stage.ordinal();
        if (s >= Stage.ENCRYPTION.ordinal() && TLS_FLOWS.contains(f.id())) {
            lines.add(f.id() + ".controls.isEncrypted = True");
            lines.add(f.id() + ".controls.usesEncryptionAlgorithm = \"AES-256-GCM\"");
            lines.add(f.id() + ".controls.providesIntegrity = True");
            lines.add(f.id() + ".controls.providesConfidentiality = True");
        }
        if (s >= Stage.AUTHENTICATION.ordinal() && AUTH_FLOWS.contains(f.id())) {
            lines.add(f.id() + ".controls.authenticatesSource = True");
            lines.add(f.id() + ".controls.authenticatesDestination = True");
            lines.add(f.id() + ".controls.authorizesSource = True");
        }
        return lines;
    }
}
