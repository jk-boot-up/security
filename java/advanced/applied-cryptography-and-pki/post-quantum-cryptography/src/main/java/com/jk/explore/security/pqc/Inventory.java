package com.jk.explore.security.pqc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Counts, in the main source of the beginner and intermediate tracks, how many projects name each cryptographic algorithm. */
final class Inventory {

    /** Algorithms in the order they are printed. The first group is broken by a large quantum computer, the second is not. */
    static final Map<String, Pattern> PUBLIC_KEY = new LinkedHashMap<>();
    static final Map<String, Pattern> SYMMETRIC = new LinkedHashMap<>();

    static {
        PUBLIC_KEY.put("RSA", Pattern.compile("\\bRSA\\b|withRSA|\"RSA/"));
        PUBLIC_KEY.put("ECDSA", Pattern.compile("ECDSA"));
        PUBLIC_KEY.put("ECDH", Pattern.compile("ECDH"));
        PUBLIC_KEY.put("EC key pair (any use)", Pattern.compile("\"EC\""));
        PUBLIC_KEY.put("X25519", Pattern.compile("X25519|\"XDH\""));
        PUBLIC_KEY.put("Ed25519", Pattern.compile("Ed25519|EdDSA"));
        PUBLIC_KEY.put("finite-field Diffie-Hellman", Pattern.compile("\"DH\"|\"DiffieHellman\""));
        SYMMETRIC.put("AES", Pattern.compile("\\bAES\\b|\"AES/"));
        SYMMETRIC.put("SHA-256", Pattern.compile("SHA-256|SHA256"));
        SYMMETRIC.put("HMAC", Pattern.compile("Hmac"));
    }

    /** What was scanned and what was found. */
    record Result(int projects, int files, Map<String, Set<String>> projectsByAlgorithm, Set<String> anyPublicKey, int mlKem, int mlDsa) {
    }

    private Inventory() {
    }

    /** The repository's java directory, found by walking up from the working directory; null when this project is outside the repository. */
    static Path javaRoot(Path start) {
        for (Path p = start.toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isDirectory(p.resolve("java").resolve("beginner")) && Files.isDirectory(p.resolve("java").resolve("intermediate"))) {
                return p.resolve("java");
            }
        }
        return null;
    }

    static Result scan(Path javaRoot) throws IOException {
        Set<String> allProjects = new TreeSet<>();
        Map<String, Set<String>> byAlgorithm = new LinkedHashMap<>();
        PUBLIC_KEY.keySet().forEach(k -> byAlgorithm.put(k, new TreeSet<>()));
        SYMMETRIC.keySet().forEach(k -> byAlgorithm.put(k, new TreeSet<>()));
        Set<String> anyPublicKey = new TreeSet<>();
        Set<String> mlKem = new TreeSet<>();
        Set<String> mlDsa = new TreeSet<>();
        int files = 0;
        for (String track : List.of("beginner", "intermediate")) {
            List<Path> sources;
            try (Stream<Path> walk = Files.walk(javaRoot.resolve(track))) {
                sources = walk.filter(Inventory::isMainSource).sorted().toList();
            }
            for (Path file : sources) {
                files++;
                Path rel = javaRoot.relativize(file);
                String project = rel.getName(0) + "/" + rel.getName(1) + "/" + rel.getName(2);
                allProjects.add(project);
                String text = Files.readString(file, StandardCharsets.UTF_8);
                for (var e : PUBLIC_KEY.entrySet()) {
                    if (e.getValue().matcher(text).find()) {
                        byAlgorithm.get(e.getKey()).add(project);
                        anyPublicKey.add(project);
                    }
                }
                for (var e : SYMMETRIC.entrySet()) {
                    if (e.getValue().matcher(text).find()) {
                        byAlgorithm.get(e.getKey()).add(project);
                    }
                }
                if (text.contains("ML-KEM")) {
                    mlKem.add(project);
                }
                if (text.contains("ML-DSA")) {
                    mlDsa.add(project);
                }
            }
        }
        return new Result(allProjects.size(), files, byAlgorithm, anyPublicKey, mlKem.size(), mlDsa.size());
    }

    private static boolean isMainSource(Path p) {
        String s = p.toString().replace('\\', '/');
        return s.endsWith(".java") && s.contains("/src/main/java/") && !s.contains("/build/");
    }
}
