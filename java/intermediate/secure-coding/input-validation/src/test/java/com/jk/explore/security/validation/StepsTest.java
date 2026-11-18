package com.jk.explore.security.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/** The teaching steps end where the real classes end, and the models count what the demo says they count. */
class StepsTest {

    @ParameterizedTest
    @ValueSource(strings = {"2", "99", "7"})
    void quantityStepsEndInTheSameValueAsParse(String raw) throws Rejected {
        List<Steps.Step> steps = Steps.quantity(raw);
        assertEquals(Quantity.parse(raw).toString(), steps.getLast().result());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-3", "0", "100", "1e2", "1234", ""})
    void quantityStepsRefuseWhatParseRefuses(String raw) {
        assertThrows(Rejected.class, () -> Quantity.parse(raw));
        assertTrue(Steps.quantity(raw).getLast().result().endsWith("refused"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana", "ａｎａ", "ANA", " Ana.B "})
    void usernameStepsEndInTheCanonicalName(String raw) throws Rejected {
        assertEquals(Username.parse(raw).value(), Steps.username(raw).getLast().result());
    }

    @Test
    void codePointsAndVisibleShowHiddenCharacters() {
        assertEquals("U+0061 U+006E", Steps.codePoints("an"));
        assertEquals("an[U+200B]a", Steps.visible("an​a"));
    }

    @Test
    void nestedPatternTriesEverySplitOfTheRun() {
        Backtracking engine = new Backtracking();
        for (int n = 1; n <= 8; n++) {
            engine.nested("a".repeat(n) + "!");
            assertEquals(1 << (n - 1), engine.splits().size());
        }
        engine.nested("aaa!");
        assertEquals(List.of("(aaa)!", "(aa)(a)!", "(a)(aa)!", "(a)(a)(a)!"), engine.splits());
    }

    @Test
    void trustingShopUsesTheHiddenPriceWhenThereIsOne() {
        assertEquals(new BigDecimal("0.01"), TrustingShop.price(Map.of("sku", "SHO-1042", "price", "0.01")));
        assertEquals(new BigDecimal("89.90"), TrustingShop.price(Map.of("sku", "SHO-1042")));
    }
}
