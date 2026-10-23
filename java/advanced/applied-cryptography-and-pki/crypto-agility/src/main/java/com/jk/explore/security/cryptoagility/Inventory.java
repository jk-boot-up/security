package com.jk.explore.security.cryptoagility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Finds the algorithm names that source code asks the JDK for: the first step to knowing what has to change. */
public final class Inventory {

    /** One use of an algorithm: in which file, and what the policy says. */
    public record Use(String file, String algorithm, Status status) {
    }

    private static final Pattern GET_INSTANCE = Pattern.compile("(?:Cipher|MessageDigest|Mac|Signature|SecureRandom|SecretKeyFactory|KeyGenerator)\\.getInstance\\(\"([^\"]+)\"");

    private Inventory() {
    }

    public static List<Use> scan(Path dir) throws IOException {
        List<Use> uses = new ArrayList<>();
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).sorted().toList()) {
                Matcher m = GET_INSTANCE.matcher(Files.readString(p));
                while (m.find()) {
                    uses.add(new Use(p.getFileName().toString(), m.group(1), Policy.of(m.group(1))));
                }
            }
        }
        return uses;
    }
}
