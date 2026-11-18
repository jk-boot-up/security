package com.jk.explore.security.sqlihibernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** No database needed: the query text each search builds, and how the demo lays it out. */
class QueryTextTest {

    @Test
    void aQuoteInThePastedJpqlEndsTheLiteral() {
        String jpql = UnsafeSearch.jpqlStatement(SqlHibernateDemo.JPQL_BYPASS, "p.name");
        assertTrue(jpql.contains("like '%zzz' or p.listed = false or p.name like 'zzz%' and p.listed = true"));
    }

    @Test
    void thePastedNativeSqlEndsInAComment() {
        String sql = UnsafeSearch.nativeStatement(SqlHibernateDemo.UNION);
        assertTrue(sql.contains("like '%zzz' union select email || ' ' || password_hash from users --%'"));
    }

    @Test
    void theFixedJpqlNeverContainsTheCustomersText() {
        assertEquals("select p.name from Product p where p.name like :term and p.listed = true order by p.name",
                SafeSearch.jpqlStatement(SqlHibernateDemo.SORT_TRICK));
        assertEquals("select p.name from Product p where p.name like :term and p.listed = true order by p.price", SafeSearch.jpqlStatement("price"));
        assertFalse(SafeSearch.NATIVE.contains("'"));
    }

    @Test
    void theLayoutKeepsTheStatementAndFitsTheScreen() {
        for (String statement : List.of(UnsafeSearch.jpqlStatement(SqlHibernateDemo.JPQL_ORACLE_TRUE, "p.name"),
                UnsafeSearch.nativeStatement(SqlHibernateDemo.UNION), SafeSearch.jpqlStatement("name"))) {
            List<String> lines = SqlHibernateDemo.layout(statement);
            assertEquals(statement, String.join(" ", lines.stream().map(String::strip).toList()));
            assertTrue(lines.stream().allMatch(l -> l.length() <= SqlHibernateDemo.WIDTH - 4), lines.toString());
        }
    }

    @Test
    void theCommentStaysOnOneLastLine() {
        List<String> lines = SqlHibernateDemo.layout(UnsafeSearch.nativeStatement(SqlHibernateDemo.UNION));
        assertEquals("--%' and listed = true order by name", lines.getLast());
    }
}
