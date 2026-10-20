package com.jk.explore.security.xmlinj;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXParseException;

/** Reads {@code <invoice><customer/><total/><note/></invoice>}. The two subclasses differ only in how the XML parser is configured. */
public abstract class InvoiceParser {

    /** The parser factory, configured the way this version trusts the upload. */
    protected abstract DocumentBuilderFactory factory() throws Exception;

    public Invoice parse(String xml) {
        try {
            DocumentBuilder builder = factory().newDocumentBuilder();
            builder.setErrorHandler(null);
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Element root = doc.getDocumentElement();
            if (!root.getTagName().equals("invoice")) {
                throw new InvoiceException("not an invoice");
            }
            return new Invoice(text(root, "customer"), text(root, "total"), text(root, "note"));
        } catch (SAXParseException e) {
            if (String.valueOf(e.getMessage()).contains("JAXP00010001")) {
                throw new InvoiceException("stopped by the parser's entity expansion limit");
            }
            throw new InvoiceException("not well-formed: " + firstLine(e.getMessage()));
        } catch (InvoiceException e) {
            throw e;
        } catch (Exception e) {
            throw new InvoiceException("could not read the invoice: " + firstLine(e.getMessage()));
        }
    }

    private static String text(Element root, String tag) {
        var nodes = root.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private static String firstLine(String s) {
        String line = s == null ? "" : s.lines().findFirst().orElse("");
        return line.replaceAll("^\\[Fatal Error\\].*?: ", "");
    }
}
