package com.jk.explore.security.reverseeng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.classfile.ClassFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClassFileTest {

    private final Sample plain = compile("samples/plain", "bank.Main", "run");
    private final Sample obf = compile("samples/obfuscated", "a.b", "a");

    private static Sample compile(String dir, String cls, String method) {
        try {
            return Sample.compile(Path.of(dir), cls, method);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void theSamplesCompileAndRunWithTheSameBehaviour() throws Exception {
        assertEquals(plain.run("bankapp-demo-key-0001"), obf.run("bankapp-demo-key-0001"));
        assertEquals("denied", plain.run("x"));
        assertEquals("denied", obf.run(null));
    }

    @Test
    void theKeyIsInTheConstantPoolOfThePlainSampleOnly() throws Exception {
        boolean plainHas = false;
        for (Path f : plain.classFiles()) {
            plainHas |= Pool.strings(f).contains("bankapp-demo-key-0001");
        }
        boolean obfHas = false;
        for (Path f : obf.classFiles()) {
            obfHas |= Pool.strings(f).contains("bankapp-demo-key-0001");
        }
        assertTrue(plainHas);
        assertFalse(obfHas);
    }

    @Test
    void theKeyCheckIsFoundByShapeInBothSamples() throws Exception {
        for (Sample s : List.of(plain, obf)) {
            long found = 0;
            for (Path f : s.classFiles()) {
                if (Pool.keyCheck(ClassFile.of().parse(Files.readAllBytes(f))) != null) {
                    found++;
                }
            }
            assertEquals(1, found);
        }
    }

    @Test
    void patchingMakesAnyKeyPassInBothSamples() throws Exception {
        for (Sample s : List.of(plain, obf)) {
            Path out = Files.createTempDirectory("p");
            assertEquals(1, Patcher.patchAll(s.classes(), out));
            assertTrue(s.with(out).run("wrong").startsWith("licensed"));
            assertTrue(s.with(out).run(null).startsWith("licensed"));
        }
    }

    @Test
    void theOriginalClassFilesAreNotChangedByPatching() throws Exception {
        Path out = Files.createTempDirectory("p");
        Patcher.patchAll(plain.classes(), out);
        assertEquals("denied", plain.run("wrong"));
    }

    @Test
    void interestingStringsNameKeysLicencesAndAddresses() {
        assertTrue(Pool.interesting("Licence server"));
        assertTrue(Pool.interesting("https://x.example"));
        assertTrue(Pool.interesting("API-KEY"));
        assertFalse(Pool.interesting("Corner Bakery"));
        assertFalse(Pool.interesting("1a2b3c"));
    }

    @Test
    void namesIncludeClassesMethodsAndFields() throws Exception {
        List<String> names = Pool.names(plain.classFiles());
        assertTrue(names.contains("bank/Licence"));
        assertTrue(names.contains("check"));
        assertTrue(names.contains("VENDOR_KEY"));
        assertNotNull(names);
    }
}
