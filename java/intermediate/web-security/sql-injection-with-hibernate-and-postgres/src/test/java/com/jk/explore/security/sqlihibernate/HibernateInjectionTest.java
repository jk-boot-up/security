package com.jk.explore.security.sqlihibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.JDBCException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Runs against a real PostgreSQL in a container that is removed afterwards: ./gradlew integrationTest */
@Tag("integration")
class HibernateInjectionTest {

    private static PostgresLab lab;
    private static SessionFactory admin;
    private static SessionFactory searchUser;
    private static UnsafeSearch unsafe;
    private static SafeSearch safe;

    @BeforeAll
    static void setUp() {
        lab = PostgresLab.start();
        Shop.load(lab);
        admin = Shop.admin(lab);
        searchUser = Shop.searchUser(lab);
        unsafe = new UnsafeSearch(admin);
        safe = new SafeSearch(admin);
    }

    @AfterAll
    static void tearDown() {
        if (searchUser != null) {
            searchUser.close();
        }
        if (admin != null) {
            admin.close();
        }
        if (lab != null) {
            lab.close();
        }
    }

    @Test
    void honestSearchWorksInAllVersions() {
        List<String> expected = List.of("Blue Kettle", "Red Kettle");
        assertEquals(expected, unsafe.jpql("Kettle", "p.price"));
        assertEquals(expected, safe.jpql("Kettle", "price"));
        assertEquals(expected, safe.criteria("Kettle", "price"));
        assertEquals(expected, safe.nativeSql("Kettle"));
    }

    @Test
    void concatenatedJpqlShowsAnUnlistedProduct() {
        assertEquals(List.of("Prototype Toaster"), unsafe.jpql(SqlHibernateDemo.JPQL_BYPASS, "p.name"));
    }

    @Test
    void concatenatedJpqlAnswersQuestionsAboutOtherEntities() {
        assertEquals(4, unsafe.jpql(SqlHibernateDemo.JPQL_ORACLE_TRUE, "p.name").size());
        assertTrue(unsafe.jpql(SqlHibernateDemo.JPQL_ORACLE_FALSE, "p.name").isEmpty());
    }

    @Test
    void concatenatedNativeSqlLeaksTheHashes() {
        List<String> leaked = unsafe.nativeSql(SqlHibernateDemo.UNION);
        assertTrue(leaked.contains("ana@shop.example " + Shop.SECRET_HASH));
        assertEquals(2, leaked.size());
    }

    @Test
    void parametersKeepEveryPayloadAsText() {
        assertTrue(safe.jpql(SqlHibernateDemo.JPQL_BYPASS, "name").isEmpty());
        assertTrue(safe.jpql(SqlHibernateDemo.JPQL_ORACLE_TRUE, "name").isEmpty());
        assertTrue(safe.nativeSql(SqlHibernateDemo.UNION).isEmpty());
        assertTrue(safe.criteria(SqlHibernateDemo.JPQL_BYPASS, "name").isEmpty());
    }

    @Test
    void sortKeyThatIsNotOnTheListFallsBackToTheDefault() {
        assertEquals(List.of("Blue Kettle", "Red Kettle"), safe.jpql("Kettle", SqlHibernateDemo.SORT_TRICK));
        assertEquals(List.of("Blue Kettle", "Red Kettle"), safe.criteria("Kettle", "p.passwordHash"));
        assertEquals(4, admin.fromSession(session -> session.createQuery("select count(p) from Product p", Long.class).getSingleResult()).intValue());
    }

    @Test
    void sortKeyPastedIntoJpqlIsStillQueryLanguage() {
        // The sort is the second hole: a concatenated sort is read as JPQL, here a subquery that decides the order.
        List<String> rows = unsafe.jpql("Kettle", "(select count(a) from Account a where a.email like 'ana%'), p.name");
        assertEquals(List.of("Blue Kettle", "Red Kettle"), rows);
    }

    @Test
    void aSearchUserWithFewRightsCannotReadTheAccountTable() {
        UnsafeSearch careless = new UnsafeSearch(searchUser);
        JDBCException refused = assertThrows(JDBCException.class, () -> careless.nativeSql(SqlHibernateDemo.UNION));
        assertTrue(refused.getCause().getMessage().contains("permission denied for table users"));
        assertThrows(RuntimeException.class, () -> careless.jpql(SqlHibernateDemo.JPQL_ORACLE_TRUE, "p.name"));
        assertFalse(careless.nativeSql("Kettle").isEmpty());
    }
}
