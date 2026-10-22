package com.jk.explore.security.racepg;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

/** Hibernate ORM 7 against the lab's database, remembering every SQL statement it sends. */
final class Hib implements AutoCloseable {

    final List<String> sql = new CopyOnWriteArrayList<>();
    final SessionFactory factory;

    Hib(Db db) {
        Configuration cfg = new Configuration().addAnnotatedClass(HibAccount.class);
        cfg.setProperty(AvailableSettings.JAKARTA_JDBC_URL, db.url());
        cfg.setProperty(AvailableSettings.JAKARTA_JDBC_USER, "postgres");
        cfg.setProperty(AvailableSettings.JAKARTA_JDBC_PASSWORD, Db.PASSWORD);
        cfg.setProperty(AvailableSettings.HBM2DDL_AUTO, "create-drop");
        cfg.setProperty(AvailableSettings.POOL_SIZE, "4");
        cfg.getProperties().put(AvailableSettings.STATEMENT_INSPECTOR, (org.hibernate.resource.jdbc.spi.StatementInspector) statement -> {
            sql.add(statement);
            return statement;
        });
        this.factory = cfg.buildSessionFactory();
    }

    @Override
    public void close() {
        factory.close();
    }
}
