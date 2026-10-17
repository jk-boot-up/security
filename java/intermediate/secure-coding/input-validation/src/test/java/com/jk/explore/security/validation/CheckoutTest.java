package com.jk.explore.security.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CheckoutTest {

    private static Map<String, String> with(String field, String value) {
        Map<String, String> fields = new HashMap<>(ValidationDemo.honest());
        fields.put(field, value);
        return fields;
    }

    @Test
    void anHonestOrderIsPricedFromTheCatalogue() {
        Checkout.Result result = Checkout.place(ValidationDemo.honest());
        assertTrue(result.accepted());
        assertEquals(new BigDecimal("179.80"), result.order().total());
    }

    @Test
    void theBrowserChecksAreSkippedByAnyDirectRequest() {
        Map<String, String> negative = with("quantity", "-3");
        assertFalse(BrowserForm.wouldSubmit(negative));
        assertEquals(new BigDecimal("-269.70"), TrustingShop.total(negative));
        assertFalse(Checkout.place(negative).accepted());
    }

    @Test
    void anUnexpectedFieldIsRefusedAndThePriceNeverComesFromTheForm() {
        Map<String, String> cheap = with("price", "0.01");
        assertEquals(new BigDecimal("0.02"), TrustingShop.total(cheap));
        Checkout.Result result = Checkout.place(cheap);
        assertFalse(result.accepted());
        assertEquals("price", result.problems().get(0).field());
    }

    @Test
    void anUnknownProductIsRefusedEvenWithAValidShape() {
        Checkout.Result result = Checkout.place(with("sku", "TOY-0001"));
        assertFalse(result.accepted());
        assertEquals("not a product the store sells", result.problems().get(0).rule());
    }

    @Test
    void everyProblemIsReportedAtOnce() {
        Checkout.Result result = Checkout.place(Map.of("sku", "x", "quantity", "x", "email", "x", "country", "x"));
        assertEquals(4, result.problems().size());
    }

    @Test
    void aStrangeFieldNameIsNotRepeatedInTheProblem() {
        Map<String, String> fields = with("x\r\n<b>", "1");
        Checkout.Result result = Checkout.place(fields);
        assertEquals("a field", result.problems().get(0).field());
    }
}
