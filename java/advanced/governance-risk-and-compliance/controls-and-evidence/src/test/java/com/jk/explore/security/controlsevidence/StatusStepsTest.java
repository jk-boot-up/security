package com.jk.explore.security.controlsevidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StatusStepsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);
    private final Assessor assessor = new Assessor(Path.of("evidence"), TODAY);

    private static Control auto() {
        return new Control("X", "x", "o", "PR.AA-05", List.of("A.5.15"), "x.xml", "auto");
    }

    private void endsLikeTheAssessor(Control c, Optional<Evidence> e) {
        List<StatusSteps.Step> steps = StatusSteps.of(assessor, c, e);
        assertEquals(assessor.assess(c, e), steps.getLast().ends());
        steps.subList(0, steps.size() - 1).forEach(s -> assertNull(s.ends(), s.question()));
    }

    @Test
    void theLastStepIsTheAssessorsStatusForEveryControlOfTheCatalogue() throws IOException {
        for (Control c : Control.load(Path.of("catalogue/controls.csv"))) {
            endsLikeTheAssessor(c, assessor.evidenceOf(c));
        }
    }

    @Test
    void theLastStepIsTheAssessorsStatusForEveryKindOfRun() {
        for (int age : new int[] {0, 90, 91}) {
            for (int skipped : new int[] {0, 2}) {
                for (List<String> failed : List.of(List.<String>of(), List.of("t"))) {
                    endsLikeTheAssessor(auto(), Optional.of(new Evidence("s", TODAY.minusDays(age), 10, skipped, failed)));
                }
            }
        }
        endsLikeTheAssessor(auto(), Optional.empty());
    }

    @Test
    void aFailingRunStopsAtTheFailureAndNamesTheTest() {
        List<StatusSteps.Step> steps = StatusSteps.of(assessor, auto(), Optional.of(new Evidence("s", TODAY.minusDays(2), 10, 0, List.of("rejectsTlsOneOne"))));
        assertEquals(3, steps.size());
        assertEquals("1 of 10: rejectsTlsOneOne", steps.get(2).answer());
        assertEquals("2 days old, limit 90: yes", steps.get(1).answer());
    }
}
