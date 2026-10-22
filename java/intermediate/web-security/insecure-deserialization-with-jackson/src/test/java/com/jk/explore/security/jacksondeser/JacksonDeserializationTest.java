package com.jk.explore.security.jacksondeser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class JacksonDeserializationTest {

    private static final List<Line> LINES = List.of(new Line("KTL-001", 2, 2500), new Line("PAN-002", 1, 4200));

    /** Reads a crafted cookie and says whether the marker file survived. */
    private static boolean markerSurvives(JsonMapper mapper, Class<?> type, Function<String, String> crafted) throws IOException {
        Path marker = Files.createTempFile("marker", ".txt");
        try {
            try {
                Cookies.read(mapper, type, Cookies.fromJson(crafted.apply(marker.toString())));
            } catch (Refused ignored) {
                // refused or not, what matters is whether the setter ran
            }
            return Files.exists(marker);
        } finally {
            Files.deleteIfExists(marker);
        }
    }

    @Test
    void anHonestCartRoundTripsInEveryConfiguration() throws Refused {
        Cart cart = new Cart(LINES, new Note.Gift("Happy birthday"));
        for (JsonMapper mapper : new JsonMapper[] {Mappers.defaults(), Mappers.onlyOurNotesMayBeNamed()}) {
            Cart back = Cookies.read(mapper, Cart.class, Cookies.write(mapper, cart));
            assertEquals(9200, back.total());
            assertEquals(new Note.Gift("Happy birthday"), back.gift());
        }
        JsonMapper defaults = Mappers.defaults();
        TypedCart typed = new TypedCart(LINES, new Note.Coupon("SAVE5"));
        assertEquals(new Note.Coupon("SAVE5"), Cookies.read(defaults, TypedCart.class, Cookies.write(defaults, typed)).gift());
    }

    @Test
    void aNoteTypedByClassNameBuildsWhateverClassTheCookieNames() throws Exception {
        assertFalse(markerSurvives(Mappers.defaults(), Cart.class, Attacker::classNameNote));
    }

    @Test
    void jacksonThreeRefusesClassNamesAgainstAnObjectBaseType() {
        Refused refused = assertThrows(Refused.class, () -> Cookies.read(Mappers.defaults(), LooseCart.class, Cookies.fromJson(
                "{\"lines\":[],\"gift\":{\"@class\":\"" + Note.Gift.class.getName() + "\",\"message\":\"hi\"}}")));
        assertEquals("the default validator denies class names for an Object", refused.getMessage());
    }

    @Test
    void globalDefaultTypingWithAPermissiveValidatorBuildsAnyClass() throws Exception {
        assertFalse(markerSurvives(Mappers.anyClassMayBeNamed(), Object.class, Attacker::defaultTypingRoot));
    }

    @Test
    void shortNamesFromAListRefuseTheGadget() throws Exception {
        Path marker = Files.createTempFile("marker", ".txt");
        try {
            Refused refused = assertThrows(Refused.class,
                    () -> Cookies.read(Mappers.defaults(), TypedCart.class, Cookies.fromJson(Attacker.typeIdNote(marker.toString()))));
            assertEquals("unknown type id", refused.getMessage());
            assertTrue(Files.exists(marker));
        } finally {
            Files.deleteIfExists(marker);
        }
    }

    @Test
    void aValidatorThatNamesTheAllowedClassesRefusesTheGadget() throws Exception {
        Path marker = Files.createTempFile("marker", ".txt");
        try {
            Refused refused = assertThrows(Refused.class,
                    () -> Cookies.read(Mappers.onlyOurNotesMayBeNamed(), Cart.class, Cookies.fromJson(Attacker.classNameNote(marker.toString()))));
            assertEquals("class not allowed", refused.getMessage());
            assertTrue(Files.exists(marker));
        } finally {
            Files.deleteIfExists(marker);
        }
    }

    @Test
    void aForgedPriceSurvivesTheTypeFixesBecauseThePriceIsInTheCookie() throws Refused {
        for (JsonMapper mapper : new JsonMapper[] {Mappers.defaults(), Mappers.onlyOurNotesMayBeNamed()}) {
            assertEquals(2, Cookies.read(mapper, Cart.class, Cookies.fromJson(Attacker.cheapCart())).total());
        }
    }

    @Test
    void aRequestThatCarriesNoPricesAndNoTypesCannotBeForged() throws Refused {
        JsonMapper strict = Mappers.strictRequest();
        OrderRequest order = Cookies.read(strict, OrderRequest.class, Cookies.fromJson("{\"items\":[{\"sku\":\"KTL-001\",\"qty\":2}]}"));
        assertEquals(1, order.items().size());
        assertEquals("unknown property price", assertThrows(Refused.class, () -> Cookies.read(strict, OrderRequest.class,
                Cookies.fromJson("{\"items\":[{\"sku\":\"KTL-001\",\"qty\":2,\"price\":1}]}"))).getMessage());
        assertEquals("unknown property @class", assertThrows(Refused.class, () -> Cookies.read(strict, OrderRequest.class,
                Cookies.fromJson("{\"@class\":\"x\",\"items\":[]}"))).getMessage());
        assertEquals("the cookie is not a cart", assertThrows(Refused.class, () -> Cookies.read(strict, OrderRequest.class,
                Cookies.fromJson("{\"items\":[]} {\"items\":[]}"))).getMessage());
    }

    @Test
    void theShopPricesTheOrderFromItsOwnCatalogue() throws Refused {
        assertEquals(2500, Catalogue.price("KTL-001"));
        assertThrows(Refused.class, () -> Catalogue.price("XXX-999"));
    }

    @Test
    void streamLimitsRefuseDeepTextAndNumbers() {
        JsonMapper strict = Mappers.strictRequest();
        assertEquals("nesting deeper than 10", assertThrows(Refused.class,
                () -> Cookies.read(strict, Object[].class, Cookies.fromJson(Attacker.deepArray(11)))).getMessage());
        assertEquals("text longer than 64", assertThrows(Refused.class, () -> Cookies.read(strict, OrderRequest.class,
                Cookies.fromJson("{\"items\":[{\"sku\":\"" + "K".repeat(65) + "\",\"qty\":1}]}"))).getMessage());
        assertEquals("number longer than 10", assertThrows(Refused.class, () -> Cookies.read(strict, OrderRequest.class,
                Cookies.fromJson("{\"items\":[{\"sku\":\"KTL-001\",\"qty\":12345678901}]}"))).getMessage());
        assertTrue(assertThrows(Refused.class, () -> Cookies.read(Mappers.defaults(), Object[].class,
                Cookies.fromJson(Attacker.deepArray(5000)))).getMessage().startsWith("nesting deeper than"));
    }

    @Test
    void aCookieThatIsNotBase64OrJsonIsRefused() {
        assertThrows(Refused.class, () -> Cookies.read(Mappers.defaults(), Cart.class, "!!!"));
        assertThrows(Refused.class, () -> Cookies.read(Mappers.defaults(), Cart.class, Cookies.fromJson("not json")));
    }
}
