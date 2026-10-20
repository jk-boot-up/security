package com.jk.explore.security.sqli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SqlInjectionTest {

    private final MiniDb db = Shop.database();

    private static List<Object> col(List<MiniDb.Row> rows, String c) {
        return rows.stream().map(r -> r.get(c)).toList();
    }

    @Test
    void honestSearchFindsListedProductsOnly() {
        var rows = new UnsafeSearch(Shop.adminConnection(db)).search("kettle", "price");
        assertEquals(List.of("Blue Kettle", "Red Kettle"), col(rows, "name"));
    }

    @Test
    void quoteAndCommentShowHiddenProducts() {
        var rows = new UnsafeSearch(Shop.adminConnection(db)).search(SqlInjectionDemo.BYPASS, "name");
        assertTrue(col(rows, "name").contains("Prototype Toaster"));
    }

    @Test
    void unionLeaksPasswordHashes() {
        var rows = new UnsafeSearch(Shop.adminConnection(db)).search(SqlInjectionDemo.UNION, "name");
        assertTrue(col(rows, "name").contains("ana@shop.example"));
        assertTrue(col(rows, "price").contains(Shop.SECRET_HASH));
    }

    @Test
    void parametersTreatInjectionAsPlainText() {
        var safe = new SafeSearch(Shop.adminConnection(db));
        assertTrue(safe.search(SqlInjectionDemo.BYPASS, "name").isEmpty());
        assertTrue(safe.search(SqlInjectionDemo.UNION, "name").isEmpty());
    }

    @Test
    void aSearchTermWithAQuoteStillWorks() {
        db.insert("products", "Chef's Knife", 3100L, 1L);
        assertEquals(List.of("Chef's Knife"), col(new SafeSearch(Shop.searchConnection(db)).search("Chef's", "name"), "name"));
    }

    @Test
    void sortColumnComesFromAList() {
        var safe = new SafeSearch(Shop.adminConnection(db));
        assertEquals(List.of("Blue Kettle", "Copper Pan", "Red Kettle"), col(safe.search("", "price DESC; --"), "name"));
        assertEquals(List.of("Blue Kettle", "Red Kettle", "Copper Pan"), col(safe.search("", "price"), "name"));
        assertEquals(List.of("Blue Kettle", "Red Kettle"), col(safe.search("kettle", "bogus; DROP"), "name"));
    }

    @Test
    void sortInjectionIsRefusedByTheUnsafeVersion() {
        assertThrows(SqlException.class, () -> new UnsafeSearch(Shop.adminConnection(db)).search("kettle", SqlInjectionDemo.SORT_TRICK));
    }

    @Test
    void leastPrivilegeStopsTheUnionEvenWhenTheCodeIsVulnerable() {
        var e = assertThrows(SqlException.class, () -> new UnsafeSearch(Shop.searchConnection(db)).search(SqlInjectionDemo.UNION, "name"));
        assertEquals("permission denied for table users", e.getMessage());
    }

    @Test
    void secondStatementsAreRefused() {
        assertThrows(SqlException.class, () -> Shop.adminConnection(db).query("SELECT name FROM products; SELECT email FROM users"));
    }

    @Test
    void demoShowsLeakedEmailsButNeverPrintsAHash() {
        String out = SqlInjectionDemo.run();
        assertTrue(out.contains("[ana@shop.example, ben@shop.example]"));
        assertFalse(out.contains("pbkdf2"));
    }
}
