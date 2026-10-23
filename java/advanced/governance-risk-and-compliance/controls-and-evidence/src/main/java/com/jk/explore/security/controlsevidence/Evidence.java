package com.jk.explore.security.controlsevidence;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** The result of a test run, read from a JUnit XML file: when it ran, how many tests, which failed. */
public record Evidence(String suite, LocalDate ran, int tests, int skipped, List<String> failed) {

    public static Evidence read(Path xml) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Element root = factory.newDocumentBuilder().parse(xml.toFile()).getDocumentElement();
            List<String> failed = new ArrayList<>();
            NodeList cases = root.getElementsByTagName("testcase");
            for (int i = 0; i < cases.getLength(); i++) {
                Element c = (Element) cases.item(i);
                if (c.getElementsByTagName("failure").getLength() > 0) {
                    failed.add(c.getAttribute("name"));
                }
            }
            return new Evidence(root.getAttribute("name"), LocalDate.parse(root.getAttribute("timestamp").substring(0, 10)),
                    Integer.parseInt(root.getAttribute("tests")), Integer.parseInt(root.getAttribute("skipped")), failed);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }

    /** A manual attestation: only a date, with no tests behind it. */
    public static Evidence attested(LocalDate on) {
        return new Evidence("attestation", on, 0, 0, List.of());
    }
}
