package com.jk.explore.security.sqlihibernate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.hibernate.SessionFactory;

/** The fixed searches: the statement is constant text, the customer's words travel as a parameter, and the sort comes from a list. */
public final class SafeSearch {

    private final SessionFactory sessions;

    public SafeSearch(SessionFactory sessions) {
        this.sessions = sessions;
    }

    public List<String> jpql(String term, String sort) {
        String query = "select p.name from Product p where p.name like :term and p.listed = true order by " + SortColumn.parse(sort).jpql();
        try (var session = sessions.openSession()) {
            return session.createQuery(query, String.class).setParameter("term", "%" + term + "%").getResultList();
        }
    }

    public List<String> nativeSql(String term) {
        try (var session = sessions.openSession()) {
            return session.createNativeQuery("select name from products where name like :term and listed = true order by name", String.class)
                    .setParameter("term", "%" + term + "%").getResultList();
        }
    }

    /** The Criteria API binds the values it is given as parameters, so a quote in the text stays text. */
    public List<String> criteria(String term, String sort) {
        try (var session = sessions.openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<String> query = cb.createQuery(String.class);
            Root<Product> p = query.from(Product.class);
            query.select(p.get("name")).where(cb.like(p.get("name"), "%" + term + "%"), cb.isTrue(p.get("listed")))
                    .orderBy(cb.asc(p.get(SortColumn.parse(sort).column())));
            return session.createQuery(query).getResultList();
        }
    }
}
