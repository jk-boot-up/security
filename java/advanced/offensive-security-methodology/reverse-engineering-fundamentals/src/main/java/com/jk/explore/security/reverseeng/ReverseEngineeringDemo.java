package com.jk.explore.security.reverseeng;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Takes a bank's desktop client apart the way an analyst would: run it, read it, decompile it, patch it, then do it again on an obfuscated copy. */
public class ReverseEngineeringDemo {

    static final String RIGHT = "bankapp-demo-key-0001";
    static final String WRONG = "not-the-key";
    static final Map<String, String> ROLE = Map.of("bank/Main", "starts the client", "bank/Licence", "the licence check", "bank/Statement", "formats a statement line");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    static String name(Path classes, Path file) {
        return classes.relativize(file).toString().replace(".class", "");
    }

    /** A string as a reader sees it: quoted, a value slot of a string concatenation shown as {}, cut at 40 characters. */
    static String shown(String s) {
        String t = s.replace("\u0001", "{}");
        return "\"" + (t.length() > 40 ? t.substring(0, 37) + "..." : t) + "\"";
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

    static void keys(Sample s, StringBuilder out, String indent) throws Exception {
        out.append(indent + "right key  " + s.run(RIGHT) + "\n");
        out.append(indent + "wrong key  " + s.run(WRONG) + "\n");
        out.append(indent + "no key     " + s.run(null) + "\n");
    }

    static void lines(StringBuilder out, List<String> lines) {
        lines.forEach(l -> out.append("    " + l + "\n"));
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Sample plain = Sample.compile(Path.of("samples/plain"), "bank.Main", "run");
        Sample obf = Sample.compile(Path.of("samples/obfuscated"), "a.b", "a");
        Path licence = plain.classes().resolve("bank/Licence.class");

        out.append("ONE. The program.\n");
        out.append("  THE CLIENT (" + plain.classFiles().size() + " class files, compiled from samples/plain):\n");
        for (Path f : plain.classFiles()) {
            out.append(String.format("    %-16s %s%n", name(plain.classes(), f), ROLE.get(name(plain.classes(), f))));
        }
        out.append("  THE INPUT (the key the customer types):\n");
        out.append("    right key  " + RIGHT + "\n");
        out.append("    wrong key  " + WRONG + "\n");
        out.append("    no key     (none)\n");
        out.append("  THE RESULT (what the client prints):\n");
        keys(plain, out, "    ");

        out.append("TWO. Reading it without running it.\n");
        List<String> strings = new ArrayList<>();
        for (Path f : plain.classFiles()) {
            for (String s : Pool.strings(f)) {
                strings.add(String.format("%s %-14s %s", Pool.interesting(s) ? "*" : " ", name(plain.classes(), f), shown(s)));
            }
        }
        long found = interesting(plain.classFiles());
        out.append("  THE CONSTANT POOL STRINGS (" + strings.size() + ", * = key, licence, address):\n");
        strings.forEach(s -> out.append("    " + s + "\n"));
        List<String> names = Pool.names(plain.classFiles());
        out.append("  THE NAMES (" + names.size() + " in the files):\n");
        out.append("    classes  bank/Licence  bank/Main  bank/Statement\n");
        out.append("    methods  check  describe  run  main  line  <init> x3\n");
        out.append("    fields   VENDOR_KEY  SERVER\n");
        out.append("    readable " + readableNames(names) + " of " + names.size() + " (<init> is a constructor)\n");
        String bytecode = Tools.javap(plain.classes(), "bank.Licence");
        out.append("  THE BYTECODE OF check (javap -c -p bank.Licence):\n");
        lines(out, Tools.javapMethod(bytecode, "check"));
        out.append("  WHAT IT SAYS:\n");
        out.append("    0-5    no key (null): return 0, false\n");
        out.append("    6-9    load the key from the pool, call String.equals\n");
        out.append("    12     ifeq: not equal, jump to 17: return 0, false\n");
        out.append("    15-16  equal: return 1, true\n");

        out.append("THREE. Decompiling it.\n");
        String original = Files.readString(Path.of("samples/plain/bank/Licence.java"));
        String source = Tools.decompile(licence);
        out.append("  BEFORE COMPILING (samples/plain/bank/Licence.java):\n");
        lines(out, Tools.method(original, "boolean check("));
        out.append("  AFTER DECOMPILING (CFR " + Tools.CFR_VERSION + ", from Licence.class alone):\n");
        lines(out, Tools.method(source, "boolean check("));
        out.append("  WHAT CAME BACK:\n");
        String field = source.lines().filter(l -> l.contains("VENDOR_KEY =")).findFirst().orElse("").strip();
        out.append("    the key     " + field.replace("private static final String ", "") + "\n");
        out.append("    the logic   the same, written shorter\n");
        out.append("    lost        the parameter name (key became string)\n");
        out.append("    the file    " + source.lines().filter(l -> !l.isBlank()).count() + " non-blank lines, the original "
                + original.lines().filter(l -> !l.isBlank()).count() + "\n");

        out.append("FOUR. Changing it.\n");
        Pool.Size before = Pool.size(Pool.method(licence, "check"));
        out.append("  BEFORE THE PATCH (check: " + before.instructions() + " instructions, " + before.bytes() + " bytes):\n");
        keys(plain, out, "    ");
        Path patchedDir = Files.createTempDirectory("patched");
        List<String> methods = Patcher.patchAll(plain.classes(), patchedDir);
        out.append("  THE PATCH (Patcher, with the JDK class file API):\n");
        out.append("    look for  a static method (String) -> boolean that calls\n");
        out.append("              String.equals and branches with ifeq\n");
        out.append("    found     " + String.join(", ", methods) + "\n");
        out.append("    new body  iconst_1, ireturn: push 1 (true), return it\n");
        Sample patched = plain.with(patchedDir);
        Pool.Size after = Pool.size(Pool.method(patchedDir.resolve("bank/Licence.class"), "check"));
        out.append("  AFTER THE PATCH (check: " + after.instructions() + " instructions, " + after.bytes() + " bytes):\n");
        lines(out, Tools.javapMethod(Tools.javap(patchedDir, "bank.Licence"), "check"));
        keys(patched, out, "    ");
        out.append("    the check ran on the customer's computer: it decided nothing\n");

        out.append("FIVE. Obfuscation.\n");
        List<String> obfNames = Pool.names(obf.classFiles());
        out.append("  BEFORE OBFUSCATION:\n");
        out.append("    names    bank/Licence.check  VENDOR_KEY  bank/Main.run\n");
        out.append("    readable " + readableNames(names) + " of " + names.size() + " names, " + found + " strings name a key\n");
        out.append("  AFTER OBFUSCATION (the same behaviour: " + yes(obf.run(RIGHT).equals(plain.run(RIGHT)) && obf.run(WRONG).equals(plain.run(WRONG))) + "):\n");
        out.append("    names    a/a.a  k  a/b.a\n");
        out.append("    readable " + readableNames(obfNames) + " of " + obfNames.size() + " names, " + interesting(obf.classFiles()) + " strings name a key\n");
        Path obfLicence = obf.classes().resolve("a/a.class");
        List<String> encoded = new ArrayList<>();
        for (Path f : obf.classFiles()) {
            Pool.strings(f).stream().filter(s -> s.matches("([0-9a-f]{2})+")).forEach(encoded::add);
        }
        String keyHex = Pool.strings(obfLicence).stream().filter(s -> XorSteps.decode(s).equals(RIGHT)).findFirst().orElseThrow();
        out.append("    pool     \"" + keyHex + "\"\n");
        List<XorSteps.Step> steps = XorSteps.steps(keyHex).subList(0, 4);
        out.append("  HOW ONE STRING IS DECODED (a/c.d: each byte XOR 0x" + Integer.toHexString(XorSteps.KEY) + "):\n");
        StringBuilder hex = new StringBuilder("    hex   ");
        StringBuilder xor = new StringBuilder("    XOR   ");
        StringBuilder val = new StringBuilder("    byte  ");
        StringBuilder chr = new StringBuilder("    char  ");
        for (XorSteps.Step s : steps) {
            hex.append(String.format("%-6s", s.hex()));
            xor.append(String.format("%-6s", Integer.toHexString(XorSteps.KEY)));
            val.append(String.format("%-6s", String.format("%02x", s.decoded())));
            chr.append(String.format("%-6s", s.character()));
        }
        out.append(hex.toString().stripTrailing() + "  ...\n" + xor.toString().stripTrailing() + "\n" + val.toString().stripTrailing() + "\n" + chr.toString().stripTrailing() + "\n");
        out.append("    all " + XorSteps.steps(keyHex).size() + " bytes: " + XorSteps.decode(keyHex) + "\n");
        out.append("  WHAT IS STILL THERE:\n");
        String obfSource = Tools.decompile(obfLicence);
        String obfCheck = Tools.method(obfSource, "boolean a(").stream().filter(l -> l.contains("equals")).findFirst().orElse("").strip();
        out.append("    the decompiled check  " + obfCheck + "\n");
        List<String> recovered = new ArrayList<>();
        try (var loader = new URLClassLoader(new URL[] {obf.classes().toUri().toURL()}, null)) {
            Method decoder = loader.loadClass("a.c").getDeclaredMethod("d", String.class);
            decoder.setAccessible(true);
            for (String s : encoded) {
                String plainText = (String) decoder.invoke(null, s);
                if (Pool.interesting(plainText)) {
                    recovered.add(plainText);
                }
            }
        }
        out.append("    its own decoder, a/c.d, called on the " + encoded.size() + " encoded strings:\n");
        out.append("      " + recovered.size() + " name a key, a licence or an address, as before\n");
        long checks = 0;
        for (Path f : obf.classFiles()) {
            ClassModel cm = ClassFile.of().parse(Files.readAllBytes(f));
            checks += Pool.keyCheck(cm) != null ? 1 : 0;
        }
        Path patchedObf = Files.createTempDirectory("patched-obf");
        List<String> methods2 = Patcher.patchAll(obf.classes(), patchedObf);
        out.append("    the key check, found by its shape: " + checks + ", patched: " + String.join(", ", methods2) + "\n");
        out.append("    wrong key  " + obf.with(patchedObf).run(WRONG) + "\n");
        out.append("  THE BILL:\n");
        out.append("    code on a machine you do not control can be read and\n");
        out.append("      changed: keep the decision on the server\n");
        out.append("    obfuscation raises the cost of reading, it does not stop it\n");
        out.append("    read in the order of cost: strings, names, bytecode,\n");
        out.append("      then a decompiler\n");
        return out.toString();
    }
}
