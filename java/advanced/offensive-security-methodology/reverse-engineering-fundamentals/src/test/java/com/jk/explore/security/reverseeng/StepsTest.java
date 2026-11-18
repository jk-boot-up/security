package com.jk.explore.security.reverseeng;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The teaching steps give the same results as the program and the tools, with the JDK only. */
class StepsTest {

    @Test
    void xorByHandGivesWhatTheProgramsOwnDecoderGives() throws Exception {
        Sample obf = Sample.compile(Path.of("samples/obfuscated"), "a.b", "a");
        try (var loader = new URLClassLoader(new URL[] {obf.classes().toUri().toURL()}, null)) {
            Method decoder = loader.loadClass("a.c").getDeclaredMethod("d", String.class);
            decoder.setAccessible(true);
            for (Path f : obf.classFiles()) {
                for (String s : Pool.strings(f)) {
                    if (s.matches("([0-9a-f]{2})+")) {
                        assertEquals(decoder.invoke(null, s), XorSteps.decode(s));
                    }
                }
            }
        }
        assertEquals("bank", XorSteps.decode("383b3431"));
        assertEquals(0x62, XorSteps.steps("38").get(0).decoded());
    }

    @Test
    void javapLinesAreTidied() {
        assertEquals(" 6: ldc           \"bankapp-demo-key-0001\"", Tools.tidy("         6: ldc           #9                  // String bankapp-demo-key-0001"));
        assertEquals(" 9: invokevirtual String.equals", Tools.tidy("         9: invokevirtual #11                 // Method java/lang/String.equals:(Ljava/lang/Object;)Z"));
        assertEquals("12: ifeq          17", Tools.tidy("        12: ifeq          17"));
        assertEquals(" 0: aload_0", Tools.tidy("         0: aload_0"));
    }

    @Test
    void oneMethodIsCutFromASource() throws Exception {
        List<String> check = Tools.method(Files.readString(Path.of("samples/plain/bank/Licence.java")), "boolean check(");
        assertEquals("public static boolean check(String key) {", check.get(0));
        assertEquals("}", check.get(check.size() - 1));
        assertEquals(9, check.size());
    }

    @Test
    void thePatchShrinksTheCheckToTwoInstructions() throws Exception {
        Sample plain = Sample.compile(Path.of("samples/plain"), "bank.Main", "run");
        assertEquals(new Pool.Size(12, 19), Pool.size(Pool.method(plain.classes().resolve("bank/Licence.class"), "check")));
        Path out = Files.createTempDirectory("p");
        assertEquals(List.of("bank/Licence.check"), Patcher.patchAll(plain.classes(), out));
        assertEquals(new Pool.Size(2, 2), Pool.size(Pool.method(out.resolve("bank/Licence.class"), "check")));
    }
}
