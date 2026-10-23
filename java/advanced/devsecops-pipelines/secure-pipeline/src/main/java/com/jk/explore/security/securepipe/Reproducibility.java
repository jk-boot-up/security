package com.jk.explore.security.securepipe;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Builds the same commit in two clean checkouts in different directories and compares the jars. */
final class Reproducibility {

    /** The two jar digests and what differs between the jars. */
    record Comparison(String first, String second, boolean sameEntries, List<String> differentContent, boolean differentTimes) {

        boolean identical() {
            return first.equals(second);
        }
    }

    private Reproducibility() {
    }

    static Comparison compare(Git origin) throws IOException, InterruptedException {
        Path a = jarOfFreshCheckout(origin);
        // Zip times have a resolution of two seconds, so a later build is always a different time.
        Thread.sleep(2200);
        Path b = jarOfFreshCheckout(origin);
        return compare(a, b);
    }

    private static Path jarOfFreshCheckout(Git origin) throws IOException, InterruptedException {
        return Gradle.jar(origin.cloneInto().dir);
    }

    static Comparison compare(Path a, Path b) throws IOException {
        List<String> differentContent = new ArrayList<>();
        boolean sameEntries = true;
        boolean differentTimes = false;
        try (ZipFile za = new ZipFile(a.toFile()); ZipFile zb = new ZipFile(b.toFile())) {
            List<String> namesA = names(za);
            sameEntries = namesA.equals(names(zb));
            for (String name : namesA) {
                ZipEntry ea = za.getEntry(name);
                ZipEntry eb = zb.getEntry(name);
                if (eb == null) {
                    continue;
                }
                if (ea.getCrc() != eb.getCrc()) {
                    differentContent.add(name);
                }
                if (ea.getTimeLocal() != null && !ea.getTimeLocal().equals(eb.getTimeLocal())) {
                    differentTimes = true;
                }
            }
        }
        return new Comparison(Digest.sha256(a), Digest.sha256(b), sameEntries, differentContent, differentTimes);
    }

    private static List<String> names(ZipFile z) {
        List<String> names = new ArrayList<>();
        Enumeration<? extends ZipEntry> e = z.entries();
        while (e.hasMoreElements()) {
            names.add(e.nextElement().getName());
        }
        java.util.Collections.sort(names);
        return names;
    }
}
