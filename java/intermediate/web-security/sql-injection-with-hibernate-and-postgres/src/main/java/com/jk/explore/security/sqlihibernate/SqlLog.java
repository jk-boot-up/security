package com.jk.explore.security.sqlihibernate;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Hibernate hands every SQL statement to this inspector just before it goes to the driver, so the demo can print what was really sent. */
public final class SqlLog implements StatementInspector {

    private final List<String> sent = new ArrayList<>();

    @Override
    public String inspect(String sql) {
        sent.add(sql);
        return sql;
    }

    /** The last statement Hibernate produced. */
    public String last() {
        return sent.isEmpty() ? "" : sent.getLast();
    }
}
