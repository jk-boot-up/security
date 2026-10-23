package bank;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/** Account lookups of the bank's web application. */
public class AccountController {

    private static final Set<String> COLUMNS = Set.of("balance", "currency");

    String balance(HttpServletRequest request, Statement stmt) throws SQLException {
        String id = request.getParameter("id");
        return stmt.executeQuery("SELECT balance FROM accounts WHERE id = '" + id + "'").getString(1); // VULN sql-direct
    }

    String byName(HttpServletRequest request, Statement stmt) throws SQLException {
        String name = request.getParameter("name");
        String sql = "SELECT * FROM customers WHERE name = '" + name + "'";
        return stmt.executeQuery(sql).getString(1); // VULN sql-variable
    }

    String byOwner(HttpServletRequest request, Statement stmt) throws SQLException {
        String query = Queries.byOwner(request.getParameter("owner"));
        return stmt.executeQuery(query).getString(1); // VULN sql-helper
    }

    String byNumber(HttpServletRequest request, AccountRepository repository) throws SQLException {
        return repository.find(request.getParameter("number")); // the sink is in AccountRepository
    }

    String prepared(HttpServletRequest request, Connection conn) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT balance FROM accounts WHERE id = ?");
        ps.setString(1, request.getParameter("id"));
        return ps.executeQuery().getString(1); // SAFE sql-prepared
    }

    String parsed(HttpServletRequest request, Statement stmt) throws SQLException {
        int n = Integer.parseInt(request.getParameter("n"));
        return stmt.executeQuery("SELECT balance FROM accounts WHERE id = " + n).getString(1); // SAFE sql-parsed
    }

    String constant(Statement stmt) throws SQLException {
        return stmt.executeQuery("SELECT balance FROM " + "accounts").getString(1); // SAFE sql-constant
    }

    String column(HttpServletRequest request, Statement stmt) throws SQLException {
        String column = request.getParameter("column");
        if (!COLUMNS.contains(column)) {
            throw new IllegalArgumentException("unknown column");
        }
        return stmt.executeQuery("SELECT " + column + " FROM accounts").getString(1); // SAFE sql-allowlisted
    }
}
