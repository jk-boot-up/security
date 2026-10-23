package com.jk.explore.security.reverseeng;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** A small program of the bank, compiled from source into class files, and run from a class loader so that its behaviour can be compared after changes. */
public final class Sample {

    private final Path classes;
    private final String entryClass;
    private final String entryMethod;

    private Sample(Path classes, String entryClass, String entryMethod) {
        this.classes = classes;
        this.entryClass = entryClass;
        this.entryMethod = entryMethod;
    }

    public Path classes() {
        return classes;
    }

    /** Compiles every source under the folder with the JDK compiler, for Java 21 class files that the decompiler understands. */
    public static Sample compile(Path sources, String entryClass, String entryMethod) throws IOException {
        Path out = Files.createTempDirectory("classes");
        List<String> files;
        try (Stream<Path> s = Files.walk(sources)) {
            files = s.filter(p -> p.toString().endsWith(".java")).map(Path::toString).sorted().toList();
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        java.util.ArrayList<String> args = new java.util.ArrayList<>(List.of("--release", "21", "-d", out.toString()));
        args.addAll(files);
        if (compiler.run(null, null, null, args.toArray(String[]::new)) != 0) {
            throw new IllegalStateException("the sample did not compile");
        }
        return new Sample(out, entryClass, entryMethod);
    }

    /** The same program with other class files, for example after a patch. */
    public Sample with(Path otherClasses) {
        return new Sample(otherClasses, entryClass, entryMethod);
    }

    /** Runs the entry point with a key and returns what it printed. */
    public String run(String key) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[] {classes.toUri().toURL()}, getClass().getClassLoader().getParent())) {
            Object result = loader.loadClass(entryClass).getMethod(entryMethod, String.class).invoke(null, key);
            return String.valueOf(result);
        }
    }

    public List<Path> classFiles() throws IOException {
        try (Stream<Path> s = Files.walk(classes)) {
            return s.filter(p -> p.toString().endsWith(".class")).sorted().toList();
        }
    }
}
