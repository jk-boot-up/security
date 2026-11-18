package com.jk.explore.security.securepipe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Builds the same commit in two clean checkouts in different directories and compares the jars. */
final class Reproducibility {

    /**
     * The two jar digests and what differs between the jars: the entries, the entries whose bytes differ, whether the file times differ, the time of the
     * class file in the first jar, and the text of each differing entry in both jars (each checkout's directory written as checkout 1 and checkout 2).
     */
    record Comparison(String first, String second, int entries, boolean sameEntries, List<String> differentContent, boolean differentTimes, String classTime,
            List<String> differingText) {

        boolean identical() {
            return first.equals(second);
        }
    }

    static final String CLASS_FILE = "bank/ledger/Ledger.class";

    private Reproducibility() {
    }

    static Comparison compare(Git origin) throws IOException, InterruptedException {
        Path a = origin.cloneInto().dir;
        Path jarA = Gradle.jar(a);
        // Zip times have a resolution of two seconds, so a later build is always a different time.
        Thread.sleep(2200);
        Path b = origin.cloneInto().dir;
        Path jarB = Gradle.jar(b);
        return compare(jarA, jarB, List.of(a, b));
    }

    static Comparison compare(Path a, Path b) throws IOException {
        return compare(a, b, List.of());
    }

    private static Comparison compare(Path a, Path b, List<Path> checkouts) throws IOException {
        List<String> differentContent = new ArrayList<>();
        List<String> differingText = new ArrayList<>();
        boolean differentTimes = false;
        String classTime = "";
        int entries;
        boolean sameEntries;
        try (ZipFile za = new ZipFile(a.toFile()); ZipFile zb = new ZipFile(b.toFile())) {
            List<String> namesA = names(za);
            entries = namesA.size();
            sameEntries = namesA.equals(names(zb));
            for (String name : namesA) {
                ZipEntry ea = za.getEntry(name);
                ZipEntry eb = zb.getEntry(name);
                if (name.equals(CLASS_FILE) && ea.getTimeLocal() != null) {
                    classTime = ea.getTimeLocal().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                }
                if (eb == null) {
                    continue;
                }
                if (ea.getCrc() != eb.getCrc()) {
                    differentContent.add(name);
                    differingText.add(text(za, ea, checkouts));
                    differingText.add(text(zb, eb, checkouts));
                }
                LocalDateTime ta = ea.getTimeLocal();
                if (ta != null && !ta.equals(eb.getTimeLocal())) {
                    differentTimes = true;
                }
            }
        }
        return new Comparison(Digest.sha256(a), Digest.sha256(b), entries, sameEntries, differentContent, differentTimes, classTime, differingText);
    }

    /** The entry's text with each checkout's directory replaced by its name, so that no temporary path is printed. */
    private static String text(ZipFile z, ZipEntry e, List<Path> checkouts) throws IOException {
        String text = new String(z.getInputStream(e).readAllBytes(), StandardCharsets.UTF_8).strip();
        for (int i = 0; i < checkouts.size(); i++) {
            Path dir = checkouts.get(i);
            text = text.replace(dir.toRealPath().toString(), "<checkout " + (i + 1) + ">").replace(dir.toString(), "<checkout " + (i + 1) + ">");
        }
        return text;
    }

    private static List<String> names(ZipFile z) {
        List<String> names = new ArrayList<>();
        Enumeration<? extends ZipEntry> e = z.entries();
        while (e.hasMoreElements()) {
            names.add(e.nextElement().getName());
        }
        Collections.sort(names);
        return names;
    }
}
