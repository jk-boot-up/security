package com.jk.explore.security.xmlinj;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Runs the invoice upload and e-mail template story in five numbered acts and prints exact, repeatable results. Each act prints the input
 * (the upload or the template), what the reader does with it, and the result, in separate labelled sections.
 */
public class XmlTemplateDemo {

    static final String HONEST_INVOICE = "<invoice><customer>Ana</customer><total>42.50</total><note>Thanks &amp; see you soon</note></invoice>";
    static final String BOMB = bomb();
    static final String SECRET_FILE_TEXT = "demo-secret-from-the-server";
    static final String SECRET_FILE_NAME = "secret.txt";
    static final int BOMB_LEVELS = 6;
    static final int BOMB_REFS = 10;

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Path dir = Files.createTempDirectory("xxe-demo");
        Path secret = dir.resolve(SECRET_FILE_NAME);
        try {
            Files.writeString(secret, SECRET_FILE_TEXT);
            String xxe = xxePayload(secret);
            InvoiceParser naive = new NaiveInvoiceParser();
            InvoiceParser safe = new SafeInvoiceParser();

            line(out, "ONE. An invoice upload works for honest suppliers.");
            line(out, "  THE UPLOAD (the XML file the supplier sends):");
            line(out, "    <invoice>");
            line(out, "      <customer>Ana</customer>");
            line(out, "      <total>42.50</total>");
            line(out, "      <note>Thanks &amp; see you soon</note>");
            line(out, "    </invoice>");
            line(out, "  WHAT THE PARSER DOES:");
            line(out, "    &amp; is a built-in entity: it is replaced by &");
            fields(out, naive, HONEST_INVOICE, "");

            line(out, "TWO. The parser reads what the file tells it to.");
            line(out, "  THE UPLOAD (a DOCTYPE defines the entity xxe):");
            line(out, "    <!DOCTYPE invoice [");
            line(out, "      <!ENTITY xxe SYSTEM \"file:///.../" + SECRET_FILE_NAME + "\">");
            line(out, "    ]>");
            line(out, "    <invoice><customer>Mallory</customer><total>1.00</total>");
            line(out, "      <note>&xxe;</note></invoice>");
            line(out, "  WHAT THE PARSER DOES:");
            line(out, "    opens the file that xxe names, on the server,");
            line(out, "    and puts its text where &xxe; stands");
            fields(out, naive, xxe, "A SERVER FILE");
            line(out, "  THE UPLOAD (an entity bomb, " + BOMB.length() + " characters):");
            line(out, "    <!ENTITY a0 \"lol\">");
            line(out, "    <!ENTITY a1 \"" + "&a0;".repeat(BOMB_REFS) + "\">");
            line(out, "    ... a2 to a" + (BOMB_LEVELS - 1) + " the same way, ten of the level below");
            line(out, "    <!ENTITY a" + BOMB_LEVELS + " \"" + ("&a" + (BOMB_LEVELS - 1) + ";").repeat(BOMB_REFS) + "\">");
            line(out, "    <note>&a" + BOMB_LEVELS + ";</note>");
            long copies = (long) Math.pow(BOMB_REFS, BOMB_LEVELS);
            line(out, "  WHAT IT WOULD EXPAND TO (worked out):");
            line(out, "    a" + BOMB_LEVELS + " = 10 x 10 x 10 x 10 x 10 x 10 = " + String.format("%,d", copies) + " copies of lol");
            line(out, "    " + String.format("%,d", copies) + " x 3 letters = " + String.format("%,d", copies * 3) + " characters");
            line(out, "  THE RESULT:");
            line(out, "    " + attempt(naive, BOMB));
            line(out, "  stopped by luck: the JDK's default limit, not our setting");

            line(out, "THREE. Refuse the DOCTYPE.");
            line(out, "  THE PARSER SETTINGS (read back from SafeInvoiceParser):");
            DocumentBuilderFactory f = new SafeInvoiceParser().factory();
            setting(out, "disallow-doctype-decl", f.getFeature("http://apache.org/xml/features/disallow-doctype-decl"), "<- THE FIX");
            setting(out, "secure processing", f.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING), "");
            setting(out, "external DTDs allowed", "\"" + f.getAttribute(XMLConstants.ACCESS_EXTERNAL_DTD) + "\" (none)", "");
            setting(out, "external schemas allowed", "\"" + f.getAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA) + "\" (none)", "");
            setting(out, "XInclude", f.isXIncludeAware(), "");
            setting(out, "expand entity references", f.isExpandEntityReferences(), "");
            setting(out, "largest upload", String.format("%,d", SafeInvoiceParser.MAX_BYTES) + " characters", "");
            line(out, "  THE UPLOADS AND RESULTS (the same three files):");
            line(out, "    honest invoice");
            line(out, "      -> " + attempt(safe, HONEST_INVOICE));
            line(out, "    external entity (file)");
            line(out, "      -> " + attempt(safe, xxe));
            line(out, "    entity bomb");
            line(out, "      -> " + attempt(safe, BOMB));
            String big = HONEST_INVOICE.replace("Thanks", "x".repeat(SafeInvoiceParser.MAX_BYTES));
            line(out, "    an invoice of " + String.format("%,d", big.length()) + " characters");
            line(out, "      -> " + attempt(safe, big));
        } finally {
            Files.deleteIfExists(secret);
            Files.deleteIfExists(dir);
        }

        App app = App.demo();
        Customer ana = new Customer("Ana", "ana@example.test");
        Customer mallory = new Customer("${app.config.smtpPassword}", "mallory@example.test");
        String owner = "Hello ${customer.name}, your order shipped.";
        String injected = "Hello " + mallory.name() + ", your order shipped."; // the customer's text pasted into the template
        ExpressionEngine expr = new ExpressionEngine(app);

        line(out, "FOUR. A template engine that evaluates expressions.");
        line(out, "  WHAT EVERY TEMPLATE CAN REACH (customer and app):");
        line(out, String.format("    %-25s%s", "customer.name", ana.name()));
        line(out, String.format("    %-25s%s", "customer.email", ana.email()));
        line(out, String.format("    %-25s%s", "app.version", app.version()));
        line(out, String.format("    %-25s%s", "app.config.smtpPassword", app.config().smtpPassword()) + "  <- SECRET");
        line(out, "  THE OWNER'S TEMPLATE:");
        line(out, "    " + owner);
        line(out, "  THE RESULT:");
        line(out, "    " + expr.render(owner, ana));
        line(out, "  THE CUSTOMER'S NAME (typed at sign-up):");
        line(out, "    " + mallory.name());
        line(out, "  THE TEMPLATE AS BUILT (the name pasted in):");
        line(out, "    " + injected);
        line(out, "  HOW THE ENGINE WORKS IT OUT (step by step):");
        for (String step : expr.trace("app.config.smtpPassword", ana)) {
            line(out, "    " + step);
        }
        line(out, "  THE RESULT:");
        line(out, "    " + expr.render(injected, ana) + "  <- THE SECRET");
        line(out, "  ANOTHER PASTED NAME:");
        line(out, "    ${customer.class.name}");
        line(out, "  HOW THE ENGINE WORKS IT OUT (step by step):");
        for (String step : expr.trace("customer.class.name", ana)) {
            line(out, "    " + step);
        }
        line(out, "  THE SAME NAME PASSED AS DATA (fixed template):");
        line(out, "    Hello ${customer.name}  name = " + mallory.name());
        line(out, "      -> " + expr.render("Hello ${customer.name}", mallory));
        line(out, "  data is not evaluated twice; pasted text is");

        TemplateEngine plain = new LogicLessEngine();
        line(out, "FIVE. A logic-less engine keeps data as data.");
        line(out, "  THE VALUES (a flat list; nothing else is reachable):");
        line(out, "    {{name}}    {{email}}");
        line(out, "  WHAT THE ENGINE DOES:");
        line(out, "    fills each {{hole}} from the list, escaped, in one pass");
        line(out, "  THE TEMPLATES AND RESULTS:");
        line(out, "    the owner's template");
        line(out, "      -> " + plain.render("Hello {{name}}, your order shipped.", ana));
        line(out, "    the name pasted into the template");
        line(out, "      -> " + plain.render(injected, ana));
        line(out, "    the name passed as data");
        line(out, "      -> " + plain.render("Hello {{name}}", mallory));
        String markup = "<script>x</script>";
        line(out, "  THE NAME (with markup):");
        line(out, "    " + markup);
        line(out, "  HOW EACH CHARACTER IS ESCAPED (for HTML):");
        for (List<String> row : List.of(List.of("&", "<", ">"), List.of("\"", "'"))) {
            StringBuilder table = new StringBuilder("   ");
            for (String c : row) {
                table.append(" ").append(c).append(" -> ").append(LogicLessEngine.escape(c)).append("  ");
            }
            line(out, table.toString().stripTrailing());
        }
        line(out, "  THE RESULT (Hello {{name}}):");
        line(out, "    " + plain.render("Hello {{name}}", new Customer(markup, "m@example.test")));
        return out.toString();
    }

    static String xxePayload(Path file) {
        return "<?xml version=\"1.0\"?><!DOCTYPE invoice [<!ENTITY xxe SYSTEM \"" + file.toUri() + "\">]>"
                + "<invoice><customer>Mallory</customer><total>1.00</total><note>&xxe;</note></invoice>";
    }

    /** Six levels of ten references each: a few hundred bytes that would expand to a million copies. */
    static String bomb() {
        StringBuilder dtd = new StringBuilder("<!ENTITY a0 \"lol\">");
        for (int i = 1; i <= BOMB_LEVELS; i++) {
            dtd.append("<!ENTITY a").append(i).append(" \"").append(("&a" + (i - 1) + ";").repeat(BOMB_REFS)).append("\">");
        }
        return "<?xml version=\"1.0\"?><!DOCTYPE invoice [" + dtd + "]><invoice><customer>M</customer><total>1</total><note>&a"
                + BOMB_LEVELS + ";</note></invoice>";
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void setting(StringBuilder out, String name, Object value, String mark) {
        line(out, (String.format("    %-26s%s", name, value) + (mark.isEmpty() ? "" : "  " + mark)));
    }

    /** The three fields the shop stores, or the refusal, in a section of their own. */
    private static void fields(StringBuilder out, InvoiceParser parser, String xml, String mark) {
        line(out, "  THE RESULT (the fields the shop stores):");
        try {
            Invoice i = parser.parse(xml);
            line(out, "    customer  " + i.customer());
            line(out, "    total     " + i.total());
            line(out, "    note      " + i.note() + (mark.isEmpty() ? "" : "  <- " + mark));
        } catch (InvoiceException e) {
            line(out, "    refused: " + e.getMessage());
        }
    }

    private static String attempt(InvoiceParser parser, String xml) {
        try {
            Invoice i = parser.parse(xml);
            return "accepted: customer=" + i.customer() + ", total=" + i.total();
        } catch (InvoiceException e) {
            return "refused: " + e.getMessage();
        }
    }
}
