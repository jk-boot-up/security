package com.jk.explore.security.sqlihibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.JDBCException;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * No database needed: Hibernate translates each query into PostgreSQL's SQL, and the test reads that SQL before it would be sent.
 * This shows which text became SQL code and which travelled as a bound value, and proves the SQL in the golden output.
 */
class HibernateTranslationTest {

    private static final SqlLog LOG = new SqlLog();
    private static SessionFactory hibernate;

    @BeforeAll
    static void setUp() {
        hibernate = NoDatabase.factory(LOG);
    }

    @AfterAll
    static void tearDown() {
        hibernate.close();
    }

    private static String translate(Runnable search) {
        assertThrows(JDBCException.class, search::run);
        return LOG.last();
    }

    @Test
    void thePastedQuoteBecomesAnOrInTheSql() {
        String sql = translate(() -> new UnsafeSearch(hibernate).jpql(SqlHibernateDemo.JPQL_BYPASS, "p.name"));
        assertTrue(sql.contains("like '%zzz' escape '' or p1_0.listed=false or p1_0.name like 'zzz%'"), sql);
    }

    @Test
    void jpqlAboutProductsCanReachTheUsersTable() {
        String sql = translate(() -> new UnsafeSearch(hibernate).jpql(SqlHibernateDemo.JPQL_ORACLE_TRUE, "p.name"));
        assertTrue(sql.contains("exists(select a1_0.id from users a1_0 where a1_0.password_hash like 'pbkdf2$ana%'"), sql);
    }

    @Test
    void aParameterLeavesOnlyAQuestionMarkInTheSql() {
        String sql = translate(() -> new SafeSearch(hibernate).jpql(SqlHibernateDemo.JPQL_ORACLE_TRUE, "name"));
        assertEquals("select p1_0.name from products p1_0 where p1_0.name like ? escape '' and p1_0.listed=true order by p1_0.name", sql);
    }

    @Test
    void theCriteriaApiBindsItsValueToo() {
        String sql = translate(() -> new SafeSearch(hibernate).criteria(SqlHibernateDemo.JPQL_BYPASS, "name"));
        assertTrue(sql.contains("like ?"), sql);
        assertFalse(sql.contains("zzz"), sql);
    }

    @Test
    void theSqlInTheGoldenOutputIsWhatHibernateGenerates() throws Exception {
        List<String> printed = sqlSections(Files.readString(Path.of("docs", "expected-output.txt")));
        List<String> generated = List.of(
                translate(() -> new UnsafeSearch(hibernate).jpql("Kettle", "p.price")),
                translate(() -> new UnsafeSearch(hibernate).jpql(SqlHibernateDemo.JPQL_ORACLE_TRUE, "p.name")),
                translate(() -> new SafeSearch(hibernate).jpql("Kettle", "name")),
                translate(() -> new SafeSearch(hibernate).criteria(SqlHibernateDemo.JPQL_BYPASS, "name")));
        assertEquals(generated, printed);
    }

    /** The statements printed under every "THE SQL HIBERNATE SENT" or "THE SQL THE CRITERIA QUERY SENT" header, each joined back. */
    private static List<String> sqlSections(String output) {
        List<String> found = new ArrayList<>();
        List<String> current = null;
        for (String line : output.lines().toList()) {
            if (line.startsWith("  ") && !line.startsWith("    ")) {
                if (current != null) {
                    found.add(String.join(" ", current));
                }
                current = line.startsWith("  THE SQL HIBERNATE SENT") || line.startsWith("  THE SQL THE CRITERIA QUERY SENT") ? new ArrayList<>() : null;
            } else if (current != null && line.startsWith("    ")) {
                current.add(line.strip());
            }
        }
        return found;
    }
}
