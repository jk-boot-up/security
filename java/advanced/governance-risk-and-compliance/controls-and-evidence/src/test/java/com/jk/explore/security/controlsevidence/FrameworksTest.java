package com.jk.explore.security.controlsevidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrameworksTest {

    @Test
    void aCsfIdSplitsIntoFunctionCategoryAndNumber() {
        assertEquals(new Frameworks.CsfId("PR", "DS", "02"), Frameworks.CsfId.parse("PR.DS-02"));
    }

    @Test
    void anAnnexIdSplitsIntoThemeAndNumber() {
        assertEquals(new Frameworks.AnnexId(8, 24), Frameworks.AnnexId.parse("A.8.24"));
        assertEquals(new Frameworks.AnnexId(5, 1), Frameworks.AnnexId.parse("A.5.1"));
    }

    @Test
    void everyIdOfTheCatalogueAndTheApplicabilityHasAName() throws IOException {
        for (Control c : Control.load(Path.of("catalogue/controls.csv"))) {
            Frameworks.CsfId id = Frameworks.CsfId.parse(c.csf());
            assertTrue(Frameworks.FUNCTIONS.containsKey(id.function()), c.id());
            assertTrue(Frameworks.CATEGORIES.containsKey(id.function() + "." + id.category()), c.id());
            c.iso().forEach(a -> assertTrue(Frameworks.ANNEX_A.containsKey(a), a));
        }
        for (String a : Control.applicable(Path.of("catalogue/applicable.txt"))) {
            assertTrue(Frameworks.ANNEX_A.containsKey(a), a);
            assertTrue(Frameworks.THEMES.containsKey(Frameworks.AnnexId.parse(a).theme()), a);
        }
    }
}
