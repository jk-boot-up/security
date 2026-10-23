package com.jk.explore.security.controlsevidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AssessorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);
    private final Assessor assessor = new Assessor(Path.of("evidence"), TODAY);

    private static Control auto(String evidence) {
        return new Control("X", "x", "o", "PR.AA-05", List.of("A.5.15"), evidence, "auto");
    }

    private static Control manual(String date) {
        return new Control("Y", "y", "o", "GV.PO-01", List.of("A.5.1"), "attestation:" + date, "manual");
    }

    @Test
    void aFreshRunWithNoFailureIsPassing() {
        assertEquals(Assessor.Status.PASSING, assessor.assess(auto("x"), Optional.of(new Evidence("s", TODAY.minusDays(3), 10, 0, List.of()))));
    }

    @Test
    void aFailingTestMakesTheControlFailing() {
        assertEquals(Assessor.Status.FAILING, assessor.assess(auto("x"), Optional.of(new Evidence("s", TODAY.minusDays(3), 10, 0, List.of("t")))));
    }

    @Test
    void skippedTestsMakeItPartial() {
        assertEquals(Assessor.Status.PARTIAL, assessor.assess(auto("x"), Optional.of(new Evidence("s", TODAY, 10, 2, List.of()))));
    }

    @Test
    void evidenceOlderThanNinetyDaysIsStaleEvenWhenItPassed() {
        assertEquals(Assessor.Status.STALE, assessor.assess(auto("x"), Optional.of(new Evidence("s", TODAY.minusDays(91), 10, 0, List.of()))));
        assertEquals(Assessor.Status.PASSING, assessor.assess(auto("x"), Optional.of(new Evidence("s", TODAY.minusDays(90), 10, 0, List.of()))));
    }

    @Test
    void anAttestationLastsAYearAndIsNeverPassing() {
        assertEquals(Assessor.Status.ATTESTED, assessor.assess(manual("2025-10-23"), Optional.of(Evidence.attested(LocalDate.of(2025, 10, 23)))));
        assertEquals(Assessor.Status.STALE, assessor.assess(manual("2025-10-22"), Optional.of(Evidence.attested(LocalDate.of(2025, 10, 22)))));
    }

    @Test
    void noEvidenceIsNone() {
        assertEquals(Assessor.Status.NONE, assessor.assess(auto(""), Optional.empty()));
    }

    @Test
    void theCatalogueLoadsTwelveControlsEachWithAnOwnerAndAnIsoControl() throws IOException {
        List<Control> controls = Control.load(Path.of("catalogue/controls.csv"));
        assertEquals(12, controls.size());
        assertTrue(controls.stream().allMatch(c -> !c.owner().isBlank() && !c.iso().isEmpty()));
        assertEquals("PR", controls.get(0).function());
    }

    @Test
    void everyEvidenceFileOfTheCatalogueExistsAndParses() throws IOException {
        for (Control c : Control.load(Path.of("catalogue/controls.csv"))) {
            if (c.automated()) {
                assertTrue(Files.exists(Path.of("evidence", c.evidence())), c.id());
                assertTrue(Evidence.read(Path.of("evidence", c.evidence())).tests() > 0);
            }
        }
    }

    @Test
    void aTestReportWithADoctypeIsRefused() throws IOException {
        Path xml = Files.createTempFile("evil", ".xml");
        Files.writeString(xml, "<?xml version=\"1.0\"?><!DOCTYPE t [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><testsuite name=\"&x;\" tests=\"1\" skipped=\"0\" timestamp=\"2026-10-20T09:00:00\"/>");
        assertThrows(IOException.class, () -> Evidence.read(xml));
    }
}
