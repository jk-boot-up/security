package com.jk.explore.security.controlsevidence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A teaching class: the assessor's rules for one control, asked one by one in the same order, each with its answer. A test proves that the last step is
 * always the assessor's status.
 */
public final class StatusSteps {

    private StatusSteps() {
    }

    /** One rule: the question, the answer with the real values, and the status it ends in (null when it goes on to the next rule). */
    public record Step(String question, String answer, Assessor.Status ends) {
    }

    public static List<Step> of(Assessor assessor, Control c, Optional<Evidence> evidence) {
        List<Step> steps = new ArrayList<>();
        if (evidence.isEmpty()) {
            steps.add(new Step("any evidence?", "no", Assessor.Status.NONE));
            return steps;
        }
        Evidence e = evidence.get();
        steps.add(new Step("any evidence?", "yes, " + (c.automated() ? e.suite() : "an attestation"), null));
        long age = assessor.ageInDays(e);
        int limit = c.automated() ? Assessor.AUTOMATED_DAYS : Assessor.MANUAL_DAYS;
        boolean stale = age > limit;
        steps.add(new Step("fresh?", age + " days old, limit " + limit + ": " + (stale ? "no" : "yes"), stale ? Assessor.Status.STALE : null));
        if (stale) {
            return steps;
        }
        if (!c.automated()) {
            steps.add(new Step("a test?", "no, a person signed it", Assessor.Status.ATTESTED));
            return steps;
        }
        boolean failed = !e.failed().isEmpty();
        steps.add(new Step("any test failed?", e.failed().size() + " of " + e.tests() + (failed ? ": " + String.join(", ", e.failed()) : ""), failed ? Assessor.Status.FAILING : null));
        if (failed) {
            return steps;
        }
        steps.add(new Step("any test skipped?", e.skipped() + " of " + e.tests(), e.skipped() > 0 ? Assessor.Status.PARTIAL : Assessor.Status.PASSING));
        return steps;
    }
}
