package com.jk.explore.security.reverseeng;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public class ReverseEngineeringDemo {

    static final String RIGHT = "bankapp-demo-key-0001";
    static final String WRONG = "not-the-key";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    static long interesting(List<Path> files) throws IOException {
        long n = 0;
        for (Path f : files) {
            n += Pool.strings(f).stream().filter(Pool::interesting).count();
        }
        return n;
    }

    static long readableNames(List<String> names) {
        return names.stream().filter(n -> !n.equals("<init>") && !n.equals("<clinit>")).map(n -> n.substring(n.lastIndexOf('/') + 1)).filter(n -> n.length() > 1).count();
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Sample plain = Sample.compile(Path.of("samples/plain"), "bank.Main", "run");
        Sample obf = Sample.compile(Path.of("samples/obfuscated"), "a.b", "a");

        out.append("--- the program\n");
        out.append("class files: " + plain.classFiles().size() + ", the right key: " + plain.run(RIGHT).substring(0, 8) + ", a wrong key: " + plain.run(WRONG) + "\n");

        out.append("--- reading it without running it\n");
        long found = interesting(plain.classFiles());
        out.append("strings that name a key, a licence or an address in the constant pool: " + found + "\n");
        boolean keyInPool = false;
        for (Path f : plain.classFiles()) {
            keyInPool |= Pool.strings(f).contains(RIGHT);
        }
        out.append("the licence key is one of them: " + yes(keyInPool) + "\n");
        String bytecode = Tools.javap(plain.classes(), "bank.Licence");
        boolean compares = Pattern.compile("invokevirtual.*String.equals.*\\n\\s*\\d+: ifeq", Pattern.MULTILINE).matcher(bytecode).find();
        out.append("javap shows the comparison, String.equals followed by ifeq, in a method called check: " + yes(compares && bytecode.contains(" check(")) + "\n");
        out.append("names a reader meets: " + readableNames(Pool.names(plain.classFiles())) + " of " + Pool.names(plain.classFiles()).size() + " are longer than one letter\n");

        out.append("--- decompiling it\n");
        String source = Tools.decompile(plain.classes().resolve("bank/Licence.class"));
        out.append("the decompiled source has the key and the comparison: " + yes(source.contains(RIGHT) && source.contains("equals")) + ", " + source.lines().filter(l -> !l.isBlank()).count() + " non-blank lines against " + Files.readAllLines(Path.of("samples/plain/bank/Licence.java")).stream().filter(l -> !l.isBlank()).count() + " in the original\n");

        out.append("--- changing it\n");
        Path patchedDir = Files.createTempDirectory("patched");
        int methods = Patcher.patchAll(plain.classes(), patchedDir);
        Sample patched = plain.with(patchedDir);
        out.append("methods replaced by \"return true\": " + methods + ", the wrong key now: " + patched.run(WRONG).substring(0, 8) + ", no key: " + patched.run(null).substring(0, 8) + "\n");
        out.append("so a check that runs on the customer's computer decides nothing\n");

        out.append("--- obfuscation\n");
        out.append("same behaviour: right key " + yes(obf.run(RIGHT).equals(plain.run(RIGHT))) + ", wrong key " + yes(obf.run(WRONG).equals(plain.run(WRONG))) + "\n");
        out.append("strings that name a key, a licence or an address: " + interesting(obf.classFiles()) + " against " + found + "\n");
        out.append("names longer than one letter: " + readableNames(Pool.names(obf.classFiles())) + " of " + Pool.names(obf.classFiles()).size() + "\n");
        String obfSource = Tools.decompile(obf.classes().resolve("a/a.class"));
        out.append("the decompiled check is still readable as logic (a comparison with a decoded string): " + yes(obfSource.contains("equals") && obfSource.contains("c.d(")) + "\n");
        long recovered = 0;
        try (var loader = new java.net.URLClassLoader(new java.net.URL[] {obf.classes().toUri().toURL()}, null)) {
            var decoder = loader.loadClass("a.c").getDeclaredMethod("d", String.class);
            decoder.setAccessible(true);
            for (Path f : obf.classFiles()) {
                for (String s : Pool.strings(f)) {
                    if (s.matches("[0-9a-f]+") && s.length() % 2 == 0 && Pool.interesting((String) decoder.invoke(null, s))) {
                        recovered++;
                    }
                }
            }
        }
        out.append("strings recovered by calling the program's own decoder: " + recovered + "\n");
        boolean found2 = false;
        for (Path f : obf.classFiles()) {
            ClassModel cm = ClassFile.of().parse(Files.readAllBytes(f));
            found2 |= Pool.keyCheck(cm) != null;
        }
        Path patchedObf = Files.createTempDirectory("patched-obf");
        int methods2 = Patcher.patchAll(obf.classes(), patchedObf);
        out.append("the key check found by its shape although it is called a: " + yes(found2) + ", patched: " + methods2 + ", the wrong key now: " + obf.with(patchedObf).run(WRONG).substring(0, 8) + "\n");

        out.append("--- the bill\n");
        out.append("anything that runs on a machine you do not control can be read and changed: keep the decision on the server\n");
        out.append("obfuscation raises the cost of reading, it does not prevent it, and it makes your own debugging harder\n");
        out.append("read what you are given in the order of cost: strings, names, bytecode, then a decompiler\n");
        return out.toString();
    }
}
