package com.jk.explore.security.beanvalidation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** The teaching helpers read the real annotations, and the step counter counts what the engine reads. */
class RulesTest {

    @Test
    void rulesAreReadFromTheRecordsAnnotations() {
        assertEquals("@Min(1) @Max(99)", Rules.of(Line.class, "quantity"));
        assertEquals("@NotNull @Pattern([A-Z]{3}-\\d{4}) @InStock", Rules.of(Line.class, "sku"));
        assertEquals("@NotBlank @Size(max=254) @Email", Rules.of(Checkout.class, "email"));
    }

    @Test
    void aMatchThatSucceedsReadsEachCharacterAtLeastOnce() {
        Redos.STEPS.set(0);
        assertTrue(Redos.Check.find(Pattern.compile("a+$"), "aaaa"));
        assertTrue(Redos.STEPS.get() >= 4);
    }

    @Test
    void theSimplerNestedPatternGrowsSlowerThanDoubling() {
        long[] steps = new long[2];
        int[] sizes = {16, 32};
        for (int i = 0; i < 2; i++) {
            Redos.STEPS.set(0);
            Redos.Check.find(Pattern.compile("(a+)+$"), "a".repeat(sizes[i]) + "!");
            steps[i] = Redos.STEPS.get();
        }
        assertTrue(steps[1] < 8 * steps[0], "16 more a's would multiply doubling work by 65536");
    }
}
