package bank;

import java.sql.SQLException;
import java.sql.Statement;

/** Where the account queries are run. */
public class AccountRepository {

    private final Statement stmt;

    AccountRepository(Statement stmt) {
        this.stmt = stmt;
    }

    String find(String number) throws SQLException {
        return stmt.executeQuery("SELECT balance FROM accounts WHERE number = '" + number + "'").getString(1); // VULN sql-other-file
    }
}
