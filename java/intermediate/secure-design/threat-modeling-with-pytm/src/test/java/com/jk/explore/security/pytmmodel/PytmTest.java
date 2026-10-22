package com.jk.explore.security.pytmmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/** The real pytm, installed with pip into build/pytm-venv the first time, and Graphviz. Run with ./gradlew integrationTest. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PytmTest {

    private Pytm pytm;

    @BeforeAll
    void start() throws Exception {
        pytm = new Pytm(Path.of("build"));
    }

    private List<Pytm.Finding> findings(Design design, Answers.Stage stage) throws Exception {
        return pytm.findings("t-" + stage + "-" + design.flows().size(), Writer.python(design, stage, "Bank payments"));
    }

    @Test
    void theLibraryHasHundredsOfThreatsAndTheWorstCaseHasOneSeventyFindings() throws Exception {
        assertEquals(113, pytm.libraryThreats(Writer.python(Design.bank(), Answers.Stage.NONE, "x")));
        assertEquals(170, findings(Design.bank(), Answers.Stage.NONE).size());
    }

    @Test
    void everyStageOfAnswersRemovesFindings() throws Exception {
        int previous = Integer.MAX_VALUE;
        for (Answers.Stage stage : Answers.Stage.values()) {
            int now = findings(Design.bank(), stage).size();
            assertTrue(now <= previous, stage + ": " + now);
            previous = now;
        }
        assertEquals(94, previous);
    }

    @Test
    void anUnencryptedRefundFlowAddsFindingsOnTheNewFlow() throws Exception {
        List<Pytm.Finding> refunds = findings(Design.withRefunds(), Answers.Stage.RESILIENCE);
        assertEquals(99, refunds.size());
        assertEquals(5, refunds.stream().filter(f -> f.target().equals("Refund")).count());
    }

    @Test
    void pytmAcceptsTheDraftThatSkipsABoundary() throws Exception {
        List<Pytm.Finding> draft = findings(Design.draft(), Answers.Stage.NONE);
        assertTrue(draft.stream().anyMatch(f -> f.target().equals("Card vault")));
    }

    @Test
    void theDiagramHasSevenNodesEightArrowsAndFourBoundaries() throws Exception {
        String dot = pytm.dfd("t-dfd", Writer.python(Design.bank(), Answers.Stage.NONE, "x"));
        Path dotFile = Files.writeString(pytm.work().resolve("t.dot"), dot);
        Path svgFile = pytm.work().resolve("t.svg");
        Process p = new ProcessBuilder("dot", "-Tsvg", "-o", svgFile.toString(), dotFile.toString()).redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        assertEquals(0, p.waitFor());
        String svg = Files.readString(svgFile);
        assertEquals(7, svg.split("class=\"node\"", -1).length - 1);
        assertEquals(8, svg.split("class=\"edge\"", -1).length - 1);
        assertEquals(4, svg.split("class=\"cluster\"", -1).length - 1);
    }

    @Test
    void aReportListsEveryFinding() throws Exception {
        Path template = Files.writeString(pytm.work().resolve("t-template.md"), "# {tm.name}\n\n{findings:repeat:* {{item.id}} {{item.target}}: {{item.description}}\n}\n");
        Pytm.Run r = pytm.model("t-report", Writer.python(Design.bank(), Answers.Stage.RESILIENCE, "Bank payments"), "--report", template.toString());
        assertEquals(0, r.exit());
        assertEquals(94, r.output().lines().filter(l -> l.startsWith("* ")).count());
    }
}
