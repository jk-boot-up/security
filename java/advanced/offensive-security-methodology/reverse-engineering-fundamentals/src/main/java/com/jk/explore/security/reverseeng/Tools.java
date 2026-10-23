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
import java.util.List;

/** The two tools of the JDK world that read class files: javap (in the JDK) and the CFR decompiler (a jar, downloaded once and checked). */
public final class Tools {

    static final String CFR_VERSION = "0.152";
    static final String CFR_SHA256 = "f686e8f3ded377d7bc87d216a90e9e9512df4156e75b06c655a16648ae8765b2";
    static final Path CFR = Path.of("build/cfr-" + CFR_VERSION + ".jar");

    private Tools() {
    }

    public static String javap(Path classes, String className) throws IOException, InterruptedException {
        return Sh.must(List.of("javap", "-c", "-p", "-cp", classes.toString(), className));
    }

    public static String decompile(Path classFile) throws IOException, InterruptedException {
        ensureCfr();
        return Sh.run(List.of("java", "-jar", CFR.toString(), classFile.toString())).output();
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
