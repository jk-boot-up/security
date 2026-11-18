package com.jk.explore.security.reverseeng;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The two tools of the JDK world that read class files: javap (in the JDK) and the CFR decompiler (a jar, downloaded once and checked). */
public final class Tools {

    static final String CFR_VERSION = "0.152";
    static final String CFR_SHA256 = "f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2";
    static final Path CFR = Path.of("build/cfr-" + CFR_VERSION + ".jar");

    private Tools() {
    }

    /** A tool of the JDK that runs this demo, so that javap and CFR always see the same JDK. */
    static String jdk(String tool) {
        return Path.of(System.getProperty("java.home"), "bin", tool).toString();
    }

    public static String javap(Path classes, String className) throws IOException, InterruptedException {
        return Sh.must(List.of(jdk("javap"), "-c", "-p", "-cp", classes.toString(), className));
    }

    public static String decompile(Path classFile) throws IOException, InterruptedException {
        ensureCfr();
        return Sh.run(List.of(jdk("java"), "-jar", CFR.toString(), classFile.toString())).output();
    }

    /** The instructions of one method in javap -c output, tidied: no constant pool numbers, short names. */
    public static List<String> javapMethod(String javap, String method) {
        List<String> out = new ArrayList<>();
        boolean in = false;
        for (String line : javap.lines().toList()) {
            if (!in) {
                in = line.contains(" " + method + "(") && line.trim().endsWith(");");
            } else if (line.isBlank() || line.trim().equals("}")) {
                break;
            } else if (!line.trim().equals("Code:")) {
                out.add(tidy(line));
            }
        }
        return out;
    }

    /** "6: ldc  #9  // String bankapp-demo-key-0001" becomes "6: ldc  bankapp-demo-key-0001". */
    static String tidy(String line) {
        Matcher m = Pattern.compile("\\s*(\\d+): (\\S+)\\s*(#\\d+)?\\s*([^/]*?)\\s*(// (\\w+) (.*))?").matcher(line);
        if (!m.matches()) {
            return line.trim();
        }
        String operand = m.group(4);
        if (m.group(7) != null) {
            operand = m.group(6).equals("String") ? "\"" + m.group(7) + "\"" : m.group(7).replaceAll(":.*", "").replace("java/lang/", "");
        }
        return String.format("%2s: %-14s%s", m.group(1), m.group(2), operand).stripTrailing();
    }

    /** One method of a Java source (the original or a decompiled one), from its first line to its closing brace, without the indent. */
    public static List<String> method(String source, String firstLine) {
        List<String> lines = source.lines().toList();
        int start = 0;
        while (!lines.get(start).contains(firstLine)) {
            start++;
        }
        int indent = lines.get(start).length() - lines.get(start).stripLeading().length();
        List<String> out = new ArrayList<>();
        int depth = 0;
        for (int i = start; i < lines.size(); i++) {
            String l = lines.get(i);
            out.add(l.length() > indent ? l.substring(indent) : l.strip());
            depth += l.chars().filter(c -> c == '{').count() - l.chars().filter(c -> c == '}').count();
            if (depth == 0 && l.contains("}")) {
                break;
            }
        }
        return out;
    }

    static void ensureCfr() throws IOException, InterruptedException {
        if (!Files.exists(CFR)) {
            Files.createDirectories(CFR.getParent());
            HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
            http.send(HttpRequest.newBuilder(URI.create("https://github.com/leibnitz27/cfr/releases/download/" + CFR_VERSION + "/cfr-" + CFR_VERSION + ".jar")).build(), HttpResponse.BodyHandlers.ofFile(CFR));
        }
        try {
            String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(CFR)));
            if (!sha.equals(CFR_SHA256)) {
                Files.delete(CFR);
                throw new IllegalStateException("the CFR jar does not match its SHA-256");
            }
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
