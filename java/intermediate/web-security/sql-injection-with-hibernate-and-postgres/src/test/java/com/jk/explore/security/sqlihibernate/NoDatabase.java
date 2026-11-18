package com.jk.explore.security.sqlihibernate;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

/**
 * A Hibernate set up for PostgreSQL with no database behind it: it translates JPQL and Criteria into SQL, the {@link SqlLog} keeps
 * that SQL, and the statement is refused at the point where it would go over the wire. No Docker needed.
 */
final class NoDatabase {

    private static final Logger QUIET = Logger.getLogger("org.hibernate");

    private NoDatabase() {
    }

    static SessionFactory factory(SqlLog log) {
        QUIET.setLevel(Level.OFF);
        Configuration configuration = new Configuration()
                .setStatementInspector(log)
                .setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .setProperty("hibernate.boot.allow_jdbc_metadata_access", "false")
                .addAnnotatedClass(Product.class)
                .addAnnotatedClass(Account.class);
        configuration.getProperties().put("hibernate.connection.provider_class", new Provider());
        return configuration.buildSessionFactory();
    }

    /** Hands out a connection that answers simple questions and refuses to prepare any statement. */
    static final class Provider implements ConnectionProvider {

        @Override
        public Connection getConnection() {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                if (method.getName().startsWith("prepare")) {
                    throw new SQLException("no database: this test only translates");
                }
                Class<?> type = method.getReturnType();
                return type == boolean.class ? Boolean.valueOf(method.getName().equals("getAutoCommit")) : type == int.class ? Integer.valueOf(0) : null;
            });
        }

        @Override
        public void closeConnection(Connection connection) {
        }

        @Override
        public boolean supportsAggressiveRelease() {
            return false;
        }

        @Override
        public boolean isUnwrappableAs(Class<?> unwrapType) {
            return false;
        }

        @Override
        public <T> T unwrap(Class<T> unwrapType) {
            throw new UnsupportedOperationException();
        }
    }
}
