package com.jk.explore.security.sqlihibernate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/** The shop's database and the two database users it connects as. Fixed data, so every run is the same. */
public final class Shop {

    static final String SECRET_HASH = "pbkdf2$ana$9f2c";
    static final String SEARCH_USER = "shop_search";
    static final String SEARCH_PASSWORD = "lab-search-only";
    // Kept in a field: java.util.logging holds loggers weakly, and Hibernate's startup chatter would come back if this one were collected.
    private static final Logger QUIET = Logger.getLogger("org.hibernate");

    private Shop() {
    }

    /** Creates the tables as the admin user, fills them, and creates a user who may only read the product table. */
    public static void load(PostgresLab lab) {
        QUIET.setLevel(Level.OFF);
        try (SessionFactory admin = factory(lab, "postgres", PostgresLab.ADMIN_PASSWORD, true)) {
            admin.inTransaction(session -> {
                session.persist(new Product(1L, "Blue Kettle", 2500, true));
                session.persist(new Product(2L, "Red Kettle", 2900, true));
                session.persist(new Product(3L, "Copper Pan", 4200, true));
                session.persist(new Product(4L, "Prototype Toaster", 9900, false));
                session.persist(new Account(1L, "ana@shop.example", SECRET_HASH));
                session.persist(new Account(2L, "ben@shop.example", "pbkdf2$ben$41d0"));
            });
        }
        try (Connection connection = DriverManager.getConnection(lab.url(), "postgres", PostgresLab.ADMIN_PASSWORD);
                Statement statement = connection.createStatement()) {
            statement.execute("create user " + SEARCH_USER + " password '" + SEARCH_PASSWORD + "'");
            statement.execute("grant select on products to " + SEARCH_USER);
        } catch (SQLException e) {
            throw new IllegalStateException("could not create the search user", e);
        }
    }

    /** A session factory for one database user. The admin factory also creates the tables. */
    public static SessionFactory factory(PostgresLab lab, String user, String password, boolean createTables) {
        QUIET.setLevel(Level.OFF);
        Configuration configuration = new Configuration()
                .setProperty("hibernate.connection.url", lab.url())
                .setProperty("hibernate.connection.username", user)
                .setProperty("hibernate.connection.password", password)
                .setProperty("hibernate.hbm2ddl.auto", createTables ? "create" : "none")
                .addAnnotatedClass(Product.class)
                .addAnnotatedClass(Account.class);
        return configuration.buildSessionFactory();
    }

    public static SessionFactory admin(PostgresLab lab) {
        return factory(lab, "postgres", PostgresLab.ADMIN_PASSWORD, false);
    }

    public static SessionFactory searchUser(PostgresLab lab) {
        return factory(lab, SEARCH_USER, SEARCH_PASSWORD, false);
    }
}
