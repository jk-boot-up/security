package com.jk.explore.security.beanvalidation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hibernate.validator.messageinterpolation.ExpressionLanguageFeatureLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Hibernate Validator 9 with Jakarta Validation 3.1: constraints on records, groups, custom constraints and method validation. */
class BeanValidationTest {

    private Checks checks;

    @BeforeEach
    void start() {
        checks = new Checks(new Catalogue());
    }

    @AfterEach
    void stop() {
        checks.close();
    }

    private static Checkout with(String email, String country, String postalCode, String login, List<Line> lines) {
        return new Checkout(email, country, postalCode, login, lines);
    }

    @Test
    void anHonestCheckoutHasNoViolations() {
        assertEquals(List.of(), checks.check(BeanValidationDemo.honest()));
    }

    @Test
    void everyBadFieldIsReportedAtOnceWithItsPath() {
        List<String> all = checks.check(with("ben@@shop.example", "ZZ", "10115", "ben.k", List.of(new Line("sho-1042", 0))));
        assertEquals(4, all.size());
        assertTrue(all.stream().anyMatch(v -> v.startsWith("lines[0].quantity: ")));
        assertTrue(all.stream().anyMatch(v -> v.startsWith("email: ")));
    }

    @Test
    void theBuiltInEmailConstraintIsWiderThanTheShopsRule() {
        assertTrue(checks.validator().validateValue(Checkout.class, "email", "ben@localhost").isEmpty());
        assertFalse(checks.validator().validateValue(Checkout.class, "email", "ben@@shop.example").isEmpty());
    }

    @Test
    void aCountryCodeMustBeARealCapitalisedOne() {
        assertTrue(checks.validator().validateValue(Checkout.class, "country", "DE").isEmpty());
        assertFalse(checks.validator().validateValue(Checkout.class, "country", "ZZ").isEmpty());
        assertFalse(checks.validator().validateValue(Checkout.class, "country", "de").isEmpty());
    }

    @Test
    void theInvisibleCharacterRuleRefusesZeroWidthLineBreaksAndDecomposedText() {
        assertFalse(checks.validator().validate(new Handle("ben​k")).isEmpty());
        assertFalse(checks.validator().validate(new Handle("ben\nk")).isEmpty());
        assertFalse(checks.validator().validate(new Handle("josé")).isEmpty());
        assertTrue(checks.validator().validate(new Handle("josé")).isEmpty());
    }

    @Test
    void thePostalCodeMustFitTheCountryAndTheViolationIsOnThePostalCode() {
        List<String> bad = checks.check(with("ben@shop.example", "DE", "1011", "ben.k", List.of(new Line("SHO-1042", 1))));
        assertEquals(List.of("postalCode: does not look like a postal code of that country"), bad);
        assertEquals(List.of(), checks.check(with("ben@shop.example", "IN", "110001", "ben.k", List.of(new Line("SHO-1042", 1)))));
    }

    @Test
    void theCatalogueIsAskedOnlyWhenTheCheaperChecksPass() {
        checks.check(with("ben@shop.example", "DE", "10115", "ben.k", List.of(new Line("sho-1042", 1))));
        assertEquals(0, checks.catalogue().lookups());
        List<String> unknown = checks.check(with("ben@shop.example", "DE", "10115", "ben.k", List.of(new Line("SHO-9999", 1))));
        assertEquals(1, checks.catalogue().lookups());
        assertEquals(List.of("lines[0].sku: not in the catalogue"), unknown);
    }

    @Test
    void aHugeCartIsRefusedBeforeAnyLineIsLookedAt() {
        List<Line> many = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            many.add(new Line("SHO-1042", 1));
        }
        List<String> refused = checks.check(with("ben@shop.example", "DE", "10115", "ben.k", many));
        assertEquals(1, refused.size());
        assertTrue(refused.get(0).startsWith("lines: "));
        assertEquals(0, checks.catalogue().lookups());
    }

    @Test
    void theNestedPatternCostsSixteenTimesMoreForFourMoreCharactersUnlessTheLengthIsCheckedFirst() {
        Redos.STEPS.set(0);
        checks.check(new Coupon("a".repeat(8) + "!"));
        long eight = Redos.STEPS.get();
        Redos.STEPS.set(0);
        checks.check(new Coupon("a".repeat(12) + "!"));
        long twelve = Redos.STEPS.get();
        assertTrue(twelve > eight * 10, "four more characters, many times the work");
        Redos.STEPS.set(0);
        assertFalse(checks.check(new Coupon("a".repeat(5000) + "!")).isEmpty());
        assertEquals(0, Redos.STEPS.get());
    }

    @Test
    void methodValidationRefusesABadFormAndANullOne() throws Exception {
        Orders orders = new Orders(checks.catalogue());
        Method place = Orders.class.getMethod("place", Checkout.class);
        Checkout negative = with("ben@shop.example", "DE", "10115", "ben.k", List.of(new Line("SHO-1042", -3)));
        Set<ConstraintViolation<Orders>> bad = checks.validator().forExecutables().validateParameters(orders, place, new Object[] {negative}, Basic.Ordered.class);
        assertEquals(List.of("place.checkout.lines[0].quantity: must be greater than or equal to 1"), Checks.lines(bad));
        assertEquals(List.of("place.checkout: must not be null"), Checks.lines(checks.validator().forExecutables().validateParameters(orders, place, new Object[] {null}, Basic.Ordered.class)));
        assertEquals(new BigDecimal("-269.70"), orders.placeUnchecked(negative));
        assertEquals(new BigDecimal("179.80"), orders.place(BeanValidationDemo.honest()));
    }

    @Test
    void aMessageThatRepeatsTheValueRepeatsMarkup() {
        List<String> lines = Checks.lines(checks.validator().validate(new Review("<b>great</b>")));
        assertTrue(lines.get(0).contains("<b>great</b>"));
    }

    @Test
    void textInACustomTemplateIsNotEvaluatedByDefaultButIsWhenMethodCallsAreAllowed() {
        assertEquals("the coupon ${1+1} does not exist", checks.validator().validate(new Review.Unsafe("${1+1}")).iterator().next().getMessage());
        try (Checks risky = new Checks(new Catalogue(), ExpressionLanguageFeatureLevel.BEAN_METHODS)) {
            assertEquals("the coupon 2 does not exist", risky.validator().validate(new Review.Unsafe("${1+1}")).iterator().next().getMessage());
            assertEquals("the coupon ab does not exist", risky.validator().validate(new Review.Unsafe("${'a'.concat('b')}")).iterator().next().getMessage());
            assertEquals("the coupon ${1+1} does not exist", risky.validator().validate(new Review.Safe("${1+1}")).iterator().next().getMessage());
        }
    }
}
