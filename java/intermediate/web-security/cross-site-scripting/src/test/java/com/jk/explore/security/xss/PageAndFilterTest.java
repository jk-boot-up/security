package com.jk.explore.security.xss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageAndFilterTest {

    private static final Review MARKUP = new Review("Ana", "true to size", "<b>Great boots</b>");
    private static final Review QUOTE = new Review("Ben", "Fits 5\" boots", "Warm & dry, price < 20 & > 10");

    @Test
    void theUnsafePageTakesMarkupFromAReview() {
        assertEquals(1, Markup.count(ReviewPage.unsafe(List.of(MARKUP)), "b"));
    }

    @Test
    void theSafePageHasOnlyTheShopsOwnElements() {
        String page = ReviewPage.safe(List.of(MARKUP, QUOTE), "n");
        assertEquals(0, Markup.count(page, "b"));
        assertEquals(2, Markup.count(page, "div"));
        assertEquals(1, Markup.count(page, "script"));
    }

    @Test
    void theShopsScriptCarriesTheNonceAndNothingElseIsAScript() {
        String page = ReviewPage.safe(List.of(new Review("<script>x</script>", "a", "<script>y</script>")), "abc");
        assertEquals(List.of("abc"), Markup.scriptNonces(page));
    }

    @Test
    void theFilterChangesHonestTextAndMissesTheAttributeContext() {
        assertNotEquals(QUOTE.text(), NaiveFilter.clean(QUOTE.text()));
        String page = ReviewPage.unsafe(List.of(new Review("Ben", NaiveFilter.clean(QUOTE.fitNote()), "ok")));
        assertEquals("Fits 5", Markup.attribute(page, "div", "data-fit"));
    }
}
