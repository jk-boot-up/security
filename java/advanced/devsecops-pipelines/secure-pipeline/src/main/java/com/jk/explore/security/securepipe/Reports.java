package com.jk.explore.security.securepipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Reads what each gate's tool reported, so the demo can say what was found and not only that something was. Each list is sorted. */
final class Reports {

    private Reports() {
    }

    /** gitleaks' JSON report: one "rule in file:line" per finding. */
    static List<String> gitleaks(Path report) throws IOException {
        List<String> found = new ArrayList<>();
        for (JsonNode f : json(report)) {
            found.add(f.path("RuleID").asString() + " in " + f.path("File").asString() + ":" + f.path("StartLine").asInt());
        }
        return found.stream().sorted().toList();
    }

    /** trivy's JSON report: one "id severity package version" per vulnerability. */
    static List<String> trivy(Path report) throws IOException {
        List<String> found = new ArrayList<>();
        for (JsonNode result : json(report).path("Results")) {
            for (JsonNode v : result.path("Vulnerabilities")) {
                found.add(v.path("VulnerabilityID").asString() + " " + v.path("Severity").asString() + " " + v.path("PkgName").asString() + " "
                        + v.path("InstalledVersion").asString());
            }
        }
        return found.stream().sorted().toList();
    }

    /** The JUnit XML reports Gradle writes: one "test: message" per failed test. */
    static List<String> junit(Path dir) throws IOException {
        List<String> failed = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return failed;
        }
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".xml")).toList()) {
                var factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                NodeList cases = factory.newDocumentBuilder().parse(file.toFile()).getElementsByTagName("testcase");
                for (int i = 0; i < cases.getLength(); i++) {
                    Element c = (Element) cases.item(i);
                    NodeList failure = c.getElementsByTagName("failure");
                    if (failure.getLength() > 0) {
                        failed.add(c.getAttribute("name") + ": " + ((Element) failure.item(0)).getAttribute("message"));
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
        return failed.stream().sorted().toList();
    }

    private static JsonNode json(Path report) throws IOException {
        String text = Files.isRegularFile(report) ? Files.readString(report).strip() : "";
        return new JsonMapper().readTree(text.isEmpty() ? "[]" : text);
    }
}
