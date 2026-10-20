package com.jk.explore.security.xmlinj;

import javax.xml.parsers.DocumentBuilderFactory;

/** The first version: the JDK's parser as it comes, which reads a DOCTYPE and fetches external entities for whoever uploaded the file. */
public final class NaiveInvoiceParser extends InvoiceParser {

    @Override
    protected DocumentBuilderFactory factory() {
        return DocumentBuilderFactory.newInstance();
    }
}
