package com.jk.explore.security.xmlinj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XmlTemplateTest {

    @TempDir
    Path dir;

    private String xxe() throws Exception {
        Path f = dir.resolve("secret.txt");
        Files.writeString(f, XmlTemplateDemo.SECRET_FILE_TEXT);
        return XmlTemplateDemo.xxePayload(f);
    }

    @Test
    void naiveParserReadsAHonestInvoice() {
        Invoice i = new NaiveInvoiceParser().parse(XmlTemplateDemo.HONEST_INVOICE);
        assertEquals(new Invoice("Ana", "42.50", "Thanks & see you soon"), i);
    }

    @Test
    void naiveParserLeaksAFileThroughAnExternalEntity() throws Exception {
        assertEquals(XmlTemplateDemo.SECRET_FILE_TEXT, new NaiveInvoiceParser().parse(xxe()).note());
    }

    @Test
    void safeParserRefusesTheExternalEntity() throws Exception {
        var e = assertThrows(InvoiceException.class, () -> new SafeInvoiceParser().parse(xxe()));
        assertEquals("a DOCTYPE is not allowed in an invoice", e.getMessage());
    }

    @Test
    void safeParserRefusesTheBombAndHugeUploads() {
        assertThrows(InvoiceException.class, () -> new SafeInvoiceParser().parse(XmlTemplateDemo.BOMB));
        assertThrows(InvoiceException.class, () -> new SafeInvoiceParser().parse("<invoice>" + "x".repeat(20_000) + "</invoice>"));
    }

    @Test
    void safeParserStillReadsHonestInvoicesAndRejectsOtherDocuments() {
        assertEquals("Ana", new SafeInvoiceParser().parse(XmlTemplateDemo.HONEST_INVOICE).customer());
        assertThrows(InvoiceException.class, () -> new SafeInvoiceParser().parse("<order/>"));
        assertThrows(InvoiceException.class, () -> new SafeInvoiceParser().parse("<invoice>"));
    }

    @Test
    void expressionEngineLetsPastedTextReachTheApplication() {
        TemplateEngine e = new ExpressionEngine(App.demo());
        Customer ana = new Customer("Ana", "a@x.test");
        assertEquals("Hello Ana!", e.render("Hello ${customer.name}!", ana));
        assertEquals("pw=demo-smtp-password", e.render("pw=" + "${app.config.smtpPassword}", ana));
        assertTrue(e.render("${customer.class.name}", ana).endsWith(".Customer"));
    }

    @Test
    void expressionEngineDoesNotReEvaluateDataButADeveloperMustPassItAsData() {
        TemplateEngine e = new ExpressionEngine(App.demo());
        Customer mallory = new Customer("${app.config.smtpPassword}", "m@x.test");
        assertEquals("Hello ${app.config.smtpPassword}", e.render("Hello ${customer.name}", mallory));
    }

    @Test
    void logicLessEngineKeepsExpressionsAsText() {
        TemplateEngine e = new LogicLessEngine();
        Customer ana = new Customer("Ana", "a@x.test");
        assertEquals("Hello Ana", e.render("Hello {{name}}", ana));
        assertEquals("Hello ${app.config.smtpPassword}", e.render("Hello ${app.config.smtpPassword}", ana));
        assertEquals("", e.render("{{app}}", ana));
    }

    @Test
    void logicLessEngineEscapesMarkupAndDoesNotScanItsOutput() {
        TemplateEngine e = new LogicLessEngine();
        assertEquals("&lt;b&gt;&amp;&quot;&#39;", e.render("{{name}}", new Customer("<b>&\"'", "m@x.test")));
        String out = e.render("{{name}}", new Customer("{{email}}", "secret@x.test"));
        assertFalse(out.contains("secret@x.test"));
    }
}
