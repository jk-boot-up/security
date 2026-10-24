package com.jk.explore.security.depconfusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The result record and the error-text reducer are pure; only the real build needs Gradle (that part is tagged integration). */
class BuildResultTest {

    @Test
    void okWhenThereIsNoFailureAndResolvedFromReadsTheOrigin() {
        Build.Result result = new Build.Result(List.of("shop.pricing:pricing:1.4.2 from company-private"), "", "out");
        assertTrue(result.ok());
        assertTrue(result.resolvedFrom("shop.pricing:pricing:1.4.2", "company-private"));
        assertFalse(result.resolvedFrom("shop.pricing:pricing:1.4.2", "public"));
        assertEquals("shop.pricing:pricing:1.4.2 from company-private", result.toString());
    }

    @Test
    void aFailureIsNotOkAndPrintsItsReason() {
        Build.Result result = new Build.Result(List.of(), "could not find a:b:1.0", "out");
        assertFalse(result.ok());
        assertEquals("refused: could not find a:b:1.0", result.toString());
    }

    @Test
    void reasonReducesGradleErrorTextToOneSentence(@TempDir Path dir) throws IOException {
        Build build = new Build(dir, "", "");
        assertEquals("could not find com.example:thing:1.2.3",
                build.reason("A problem occurred.\nCould not find com.example:thing:1.2.3.\nSearched in the following locations:"));
        assertEquals("build failed", build.reason("some other gradle noise"));
    }
}
