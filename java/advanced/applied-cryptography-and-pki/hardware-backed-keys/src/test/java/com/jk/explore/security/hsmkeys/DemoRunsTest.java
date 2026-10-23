package com.jk.explore.security.hsmkeys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** The demo must print exactly the golden output. Needs SoftHSM2 (brew install softhsm); PINs, keys, handles and signatures are never printed. */
@Tag("integration")
class DemoRunsTest {

    @Test
    void demoOutputMatchesTheGoldenFile() throws Exception {
        String out = HardwareBackedKeysDemo.run(Path.of("build"));
        assertEquals(Files.readString(Path.of("docs", "expected-output.txt")), out);
        assertFalse(out.contains(Lab.USER_PIN) || out.contains(Lab.SO_PIN));
    }
}
