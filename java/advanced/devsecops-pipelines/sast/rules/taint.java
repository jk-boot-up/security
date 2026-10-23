import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.sql.Statement;

class TaintTests {

    void sqlDirect(HttpServletRequest request, Statement stmt) throws Exception {
        String id = request.getParameter("id");
        // ruleid: sql-taint
        stmt.executeQuery("SELECT 1 FROM t WHERE id = '" + id + "'");
    }

    void sqlThroughAVariable(HttpServletRequest request, Statement stmt) throws Exception {
        String sql = "SELECT 1 FROM t WHERE n = '" + request.getParameter("n") + "'";
        // ruleid: sql-taint
        stmt.executeQuery(sql);
    }

    void sqlParsed(HttpServletRequest request, Statement stmt) throws Exception {
        int n = Integer.parseInt(request.getParameter("n"));
        // ok: sql-taint
        stmt.executeQuery("SELECT 1 FROM t WHERE id = " + n);
    }

    void sqlConstant(Statement stmt) throws Exception {
        // ok: sql-taint
        stmt.executeQuery("SELECT 1 FROM t");
    }

    void command(HttpServletRequest request) throws Exception {
        // ruleid: command-taint
        Runtime.getRuntime().exec("convert " + request.getParameter("f"));
    }

    void commandChecked(HttpServletRequest request) throws Exception {
        // ok: command-taint
        Runtime.getRuntime().exec(Validate.fileName(request.getParameter("f")));
    }

    void path(HttpServletRequest request, File dir) throws Exception {
        // ruleid: path-taint
        new File(dir, request.getParameter("name"));
    }

    void pathChecked(HttpServletRequest request, File dir) throws Exception {
        // ok: path-taint
        new File(dir, Validate.fileName(request.getParameter("name")));
    }

    void hash() throws Exception {
        // ruleid: weak-hash
        MessageDigest.getInstance("MD5");
        // ruleid: weak-hash
        MessageDigest.getInstance("SHA-1");
        // ok: weak-hash
        MessageDigest.getInstance("SHA-256");
    }
}
