package com.jk.explore.security.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class ValueTypesTest {

    @ParameterizedTest
    @ValueSource(strings = {"1", "9", "42", "99"})
    void quantitiesFromOneToNinetyNineAreAccepted(String raw) throws Rejected {
        assertEquals(Integer.parseInt(raw), Quantity.parse(raw).value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "100", "-1", "+5", " 5", "5 ", "1e2", "٣", "", "0x10", "1000"})
    void anythingElseIsNotAQuantity(String raw) {
        assertThrows(Rejected.class, () -> Quantity.parse(raw));
    }

    @Test
    void aMissingValueIsRefused() {
        assertEquals("missing", assertThrows(Rejected.class, () -> Quantity.parse(null)).rule());
    }

    @ParameterizedTest
    @ValueSource(strings = {"sho-1042", "SHO1042", "SHO-104", "SHO-10423", "ШОE-1042", "SHO-1042\n", "SHO-١٠٤٢"})
    void productCodesHaveOneShape(String raw) {
        assertThrows(Rejected.class, () -> Sku.parse(raw));
    }

    @Test
    void emailDomainIsLowerCasedAndTheLocalPartKept() throws Rejected {
        assertEquals("Ben.Smith+shoes@shop.example", Email.parse("Ben.Smith+shoes@Shop.EXAMPLE").value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ben", "@shop.example", "ben@", "ben@@shop.example", "ben@localhost", ".ben@shop.example",
        "be..n@shop.example", "ben@-shop.example", "ben@shop..example", "ben smith@shop.example", "ben@shop.example\r\n"})
    void malformedAddressesAreRefused(String raw) {
        assertThrows(Rejected.class, () -> Email.parse(raw));
    }

    @Test
    void anAddressOverTheLimitIsRefusedBeforeItIsParsed() {
        String raw = "a".repeat(250) + "@x.example";
        assertEquals("longer than 254 characters", assertThrows(Rejected.class, () -> Email.parse(raw)).rule());
    }

    @Test
    void countryCodesComeFromTheIsoList() throws Rejected {
        assertEquals("IN", CountryCode.parse("IN").value());
        assertThrows(Rejected.class, () -> CountryCode.parse("ZZ"));
        assertThrows(Rejected.class, () -> CountryCode.parse("in"));
        assertThrows(Rejected.class, () -> CountryCode.parse("IND"));
    }

    @Test
    void moneyIsAnExactDecimalWithTwoPlaces() throws Rejected {
        assertEquals(new BigDecimal("19.50"), Money.parse("19.5").amount());
        assertEquals(2, Money.parse("7").amount().scale());
        assertEquals(new BigDecimal("10000.00"), Money.parse("10000").amount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-5", "10000.01", "19.505", "1,000", "NaN", "Infinity", "1e3", "12."})
    void otherAmountsAreRefused(String raw) {
        assertThrows(Rejected.class, () -> Money.parse(raw));
    }

    @Test
    void rejectionsNeverRepeatTheRawInput() {
        Rejected r = assertThrows(Rejected.class, () -> Email.parse("x\r\n<b>evil</b>"));
        assertFalse(r.getMessage().contains("<b>"));
        assertFalse(r.getMessage().contains("\n"));
    }
}
