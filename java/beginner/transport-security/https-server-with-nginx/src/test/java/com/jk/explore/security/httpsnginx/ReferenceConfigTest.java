package com.jk.explore.security.httpsnginx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReferenceConfigTest {

    @Test
    void theWrittenOutConfigIsWhatTheCodeGenerates() throws IOException {
        String written = Files.readString(Path.of("infra", "nginx-careful.conf"));
        assertEquals(written, NginxConfig.text(NginxConfig.Variant.CAREFUL));
    }
}
