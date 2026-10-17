package com.jk.explore.security.patchingtrivy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Reads gradle/verification-metadata.xml: the SHA-256 that Gradle expects for every artifact it downloads. */
final class VerificationMetadata {

    private VerificationMetadata() {
    }

    /** Maps `group:name:version/file` to its SHA-256, in sorted order. A DOCTYPE is refused, so the file cannot pull in anything else. */
    static Map<String, String> parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Map<String, String> checksums = new TreeMap<>();
            NodeList components = document.getElementsByTagNameNS("*", "component");
            for (int i = 0; i < components.getLength(); i++) {
                Element component = (Element) components.item(i);
                String prefix = component.getAttribute("group") + ":" + component.getAttribute("name") + ":" + component.getAttribute("version");
                NodeList artifacts = component.getElementsByTagNameNS("*", "artifact");
                for (int j = 0; j < artifacts.getLength(); j++) {
                    Element artifact = (Element) artifacts.item(j);
                    NodeList sums = artifact.getElementsByTagNameNS("*", "sha256");
                    if (sums.getLength() > 0) {
                        checksums.put(prefix + "/" + artifact.getAttribute("name"), ((Element) sums.item(0)).getAttribute("value"));
                    }
                }
            }
            return checksums;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("not a verification file: " + e.getMessage(), e);
        }
    }

    /** The artifacts whose expected checksum differs between two files, or that exist in only one. */
    static java.util.List<String> differences(Map<String, String> one, Map<String, String> other) {
        java.util.List<String> different = new java.util.ArrayList<>();
        java.util.Set<String> keys = new java.util.TreeSet<>(one.keySet());
        keys.addAll(other.keySet());
        for (String key : keys) {
            if (!java.util.Objects.equals(one.get(key), other.get(key))) {
                different.add(key);
            }
        }
        return different;
    }
}
