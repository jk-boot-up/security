package com.jk.explore.security.xmlinj;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * The fixed version: an invoice has no use for a DOCTYPE, so the parser refuses one. The other settings are belts for the case where
 * somebody switches the first one off later: no external DTDs or schemas, no XInclude, no entity expansion.
 */
public final class SafeInvoiceParser extends InvoiceParser {

    static final int MAX_BYTES = 10_000;

    @Override
    protected DocumentBuilderFactory factory() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        return f;
    }

    @Override
    public Invoice parse(String xml) {
        if (xml.length() > MAX_BYTES) {
            throw new InvoiceException("upload too large");
        }
        try {
            return super.parse(xml);
        } catch (InvoiceException e) {
            if (e.getMessage().startsWith("not well-formed: DOCTYPE") || e.getMessage().contains("DOCTYPE")) {
                throw new InvoiceException("a DOCTYPE is not allowed in an invoice");
            }
            throw e;
        }
    }
}
