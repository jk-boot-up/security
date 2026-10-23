package com.jk.explore.security.reverseeng;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.StringEntry;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** What can be read from class files without running them, with the JDK's own class file API: strings, names and the shape of methods. */
public final class Pool {

    /** The strings that the code loads, in the order of the constant pool. */
    public static List<String> strings(Path classFile) throws IOException {
        ClassModel cm = ClassFile.of().parse(Files.readAllBytes(classFile));
        List<String> out = new ArrayList<>();
        for (var entry : cm.constantPool()) {
            if (entry instanceof StringEntry s) {
                out.add(s.stringValue());
            }
        }
        return out;
    }

    /** The names of all classes, methods and fields in the files. */
    public static List<String> names(List<Path> classFiles) throws IOException {
        List<String> out = new ArrayList<>();
        for (Path p : classFiles) {
            ClassModel cm = ClassFile.of().parse(Files.readAllBytes(p));
            out.add(cm.thisClass().asInternalName());
            cm.methods().forEach(m -> out.add(m.methodName().stringValue()));
            cm.fields().forEach(f -> out.add(f.fieldName().stringValue()));
        }
        return out;
    }

    /** True when the string looks like something an attacker wants: a key, a licence, an address. */
    public static boolean interesting(String s) {
        String t = s.toLowerCase();
        return t.contains("key") || t.contains("licence") || t.contains("://");
    }

    /** The first static method that takes a String, returns a boolean and compares with String.equals and a branch: the shape of a key check, whatever it is called. */
    public static MethodModel keyCheck(ClassModel cm) {
        for (MethodModel m : cm.methods()) {
            if (m.methodTypeSymbol().descriptorString().equals("(Ljava/lang/String;)Z") && m.code().isPresent() && callsEquals(m.code().get()) && branches(m.code().get())) {
                return m;
            }
        }
        return null;
    }

    static boolean callsEquals(CodeModel code) {
        return code.elementList().stream().anyMatch(e -> e instanceof InvokeInstruction i && i.name().stringValue().equals("equals") && i.owner().asInternalName().equals("java/lang/String"));
    }

    static boolean branches(CodeModel code) {
        return code.elementList().stream().anyMatch(e -> e instanceof BranchInstruction b && b.opcode() == Opcode.IFEQ);
    }
}
