package com.jk.explore.security.xmlinj;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Runs the invoice upload and e-mail template story in five numbered acts and prints exact, repeatable results. */
public class XmlTemplateDemo {

    static final String HONEST_INVOICE = "<invoice><customer>Ana</customer><total>42.50</total><note>Thanks &amp; see you soon</note></invoice>";
    static final String BOMB = bomb();
    static final String SECRET_FILE_TEXT = "demo-secret-from-the-server";

    public static void main(String[] args) throws IOException {
        System.out.print(run());
    }

    public static String run() throws IOException {
        StringBuilder out = new StringBuilder();
        Path secret = Files.createTempFile("xxe-demo", ".txt");
        try {
            Files.writeString(secret, SECRET_FILE_TEXT);
            String xxe = xxePayload(secret);

            out.append("== 1. An invoice upload ==\n");
            line(out, "honest invoice", new NaiveInvoiceParser(), HONEST_INVOICE);

            out.append("\n== 2. The parser reads what the file tells it to ==\n");
            line(out, "external entity (file)", new NaiveInvoiceParser(), xxe);
            line(out, "entity bomb", new NaiveInvoiceParser(), BOMB);

            out.append("\n== 3. Refuse the DOCTYPE ==\n");
            line(out, "honest invoice", new SafeInvoiceParser(), HONEST_INVOICE);
            line(out, "external entity (file)", new SafeInvoiceParser(), xxe);
            line(out, "entity bomb", new SafeInvoiceParser(), BOMB);
        } finally {
            Files.deleteIfExists(secret);
        }

        App app = App.demo();
        Customer mallory = new Customer("${app.config.smtpPassword}", "mallory@example.test");
        Customer ana = new Customer("Ana", "ana@example.test");
        String owner = "Hello ${customer.name}, your order shipped.";
        String injected = "Hello " + mallory.name() + ", your order shipped."; // the customer's text pasted into the template

        out.append("\n== 4. A template engine that evaluates expressions ==\n");
        TemplateEngine expr = new ExpressionEngine(app);
        out.append(String.format("%-31s -> %s%n", "the owner's template", expr.render(owner, ana)));
        out.append(String.format("%-31s -> %s%n", "name pasted into the template", expr.render(injected, ana)));
        out.append(String.format("%-31s -> %s%n", "class name of the customer", expr.render("${customer.class.name}", ana)));
        out.append(String.format("%-31s -> %s%n", "name passed as data", expr.render("Hello ${customer.name}", mallory)));

        out.append("\n== 5. A logic-less engine keeps data as data ==\n");
        TemplateEngine plain = new LogicLessEngine();
        out.append(String.format("%-31s -> %s%n", "the owner's template", plain.render("Hello {{name}}, your order shipped.", ana)));
        out.append(String.format("%-31s -> %s%n", "name pasted into the template", plain.render(injected, ana)));
        out.append(String.format("%-31s -> %s%n", "name passed as data", plain.render("Hello {{name}}", mallory)));
        out.append(String.format("%-31s -> %s%n", "a name with markup",
                plain.render("Hello {{name}}", new Customer("<script>x</script>", "m@example.test"))));
        return out.toString();
    }

    static String xxePayload(Path file) {
        return "<?xml version=\"1.0\"?><!DOCTYPE invoice [<!ENTITY xxe SYSTEM \"" + file.toUri() + "\">]>"
                + "<invoice><customer>Mallory</customer><total>1.00</total><note>&xxe;</note></invoice>";
    }

    /** Six levels of ten references each: a few hundred bytes that would expand to a million. */
    static String bomb() {
        StringBuilder dtd = new StringBuilder("<!ENTITY a0 \"lol\">");
        for (int i = 1; i <= 6; i++) {
            dtd.append("<!ENTITY a").append(i).append(" \"").append(("&a" + (i - 1) + ";").repeat(10)).append("\">");
        }
        return "<?xml version=\"1.0\"?><!DOCTYPE invoice [" + dtd + "]><invoice><customer>M</customer><total>1</total><note>&a6;</note></invoice>";
    }

    private static void line(StringBuilder out, String label, InvoiceParser parser, String xml) {
        out.append(String.format("%-31s -> %s%n", label, attempt(parser, xml)));
    }

    private static String attempt(InvoiceParser parser, String xml) {
        try {
            Invoice i = parser.parse(xml);
            String note = i.note().length() > 40 ? i.note().substring(0, 40) + "..." : i.note();
            return "accepted: customer=" + i.customer() + ", total=" + i.total() + ", note=" + note;
        } catch (InvoiceException e) {
            return "refused: " + e.getMessage();
        }
    }
}
