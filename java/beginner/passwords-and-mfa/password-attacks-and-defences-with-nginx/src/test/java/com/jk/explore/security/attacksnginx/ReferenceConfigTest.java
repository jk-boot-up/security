package com.jk.explore.security.attacksnginx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReferenceConfigTest {

    @Test
    void theWrittenOutConfigIsWhatTheCodeGenerates() throws IOException {
        String written = Files.readString(Path.of("infra", "nginx-limit-by-address.conf"));
        assertEquals(written, NginxConfig.text(NginxConfig.Key.ADDRESS, "1r/m", 5));
    }
}
