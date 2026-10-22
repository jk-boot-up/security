package com.jk.explore.security.sqlihibernate;

import java.util.List;
import org.hibernate.SessionFactory;

/**
 * The first version of the product search, written three ways. All three paste the customer's text into the query, and Hibernate
 * cannot tell: JPQL and native SQL are just strings to it.
 */
public final class UnsafeSearch {

    private final SessionFactory sessions;

    public UnsafeSearch(SessionFactory sessions) {
        this.sessions = sessions;
    }

    /** JPQL by concatenation: the quote ends the string and the rest is query language, which can reach every entity. */
    public List<String> jpql(String term, String sort) {
        String query = "select p.name from Product p where p.name like '%" + term + "%' and p.listed = true order by " + sort;
        try (var session = sessions.openSession()) {
            return session.createQuery(query, String.class).getResultList();
        }
    }

    /** Native SQL by concatenation: the whole database language is available to whoever types in the box. */
    public List<String> nativeSql(String term) {
        String query = "select name from products where name like '%" + term + "%' and listed = true order by name";
        try (var session = sessions.openSession()) {
            return session.createNativeQuery(query, String.class).getResultList();
        }
    }
}
