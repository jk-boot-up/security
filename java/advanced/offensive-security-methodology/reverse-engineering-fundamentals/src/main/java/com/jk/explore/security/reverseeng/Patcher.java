package com.jk.explore.security.reverseeng;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.nio.file.Files;
import java.nio.file.Path;

/** Changes class files: the method that checks a key is replaced by one that says yes. This is what "cracking" a client-side check is. */
public final class Patcher {

    private Patcher() {
    }

    /** Writes a copy of the class files in which the first key check found by its shape always returns true; returns how many methods were patched. */
    public static int patchAll(Path from, Path to) throws IOException {
        int patched = 0;
        try (var files = Files.walk(from)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".class")).sorted().toList()) {
                Path target = to.resolve(from.relativize(p));
                Files.createDirectories(target.getParent());
                ClassFile cf = ClassFile.of();
                ClassModel cm = cf.parse(Files.readAllBytes(p));
                MethodModel check = Pool.keyCheck(cm);
                if (check == null) {
                    Files.copy(p, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    continue;
                }
                byte[] changed = cf.build(cm.thisClass().asSymbol(), cb -> {
                    for (var element : cm) {
                        if (element == check) {
                            cb.withMethodBody(check.methodName(), check.methodType(), check.flags().flagsMask(), code -> code.iconst_1().ireturn());
                        } else {
                            cb.with(element);
                        }
                    }
                });
                Files.write(target, changed);
                patched++;
            }
        }
        return patched;
    }
}
