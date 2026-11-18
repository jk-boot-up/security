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

    private List<ThreatTool.Finding> findings(Design design, Answers.Stage stage) throws Exception {
        return pytm.findings("t-" + stage + "-" + design.flows().size(), Writer.python(design, stage, "Bank payments"));
    }

    @Test
    void pytmListsEveryThreatOfItsLibraryThatIsNotDeprecated() throws Exception {
        assertEquals(113, pytm.library().size());
        assertEquals(113, pytm.listed(Writer.python(Design.bank(), Answers.Stage.NONE, "x")));
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
    void graphvizDrawsSevenBoxesEightArrowsAndFourBoundaries() throws Exception {
        Path svgFile = pytm.render("t", pytm.dfd("t-dfd", Writer.python(Design.bank(), Answers.Stage.NONE, "x")));
        String svg = Files.readString(svgFile);
        assertEquals(7, svg.split("class=\"node\"", -1).length - 1);
        assertEquals(8, svg.split("class=\"edge\"", -1).length - 1);
        assertEquals(4, svg.split("class=\"cluster\"", -1).length - 1);
    }
}
