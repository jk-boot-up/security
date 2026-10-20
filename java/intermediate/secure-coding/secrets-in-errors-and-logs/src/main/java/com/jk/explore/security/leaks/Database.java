package com.jk.explore.security.leaks;

import java.sql.SQLException;

/** The shop's database connection, which is down in this story. Its error message does what real driver messages do: it names the host, the user and sometimes more. */
final class Database {

    static final String URL = "jdbc:postgresql://db.internal.shop.example:5432/shop?user=app&password=s3cr3t-db-password";

    private Database() {
    }

    static void saveOrder(String orderId) throws SQLException {
        throw new SQLException("Connection to " + URL + " refused while saving order " + orderId);
    }
}
