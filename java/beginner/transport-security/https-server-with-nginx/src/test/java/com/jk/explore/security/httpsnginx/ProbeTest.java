package com.jk.explore.security.httpsnginx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ProbeTest {

    @Test
    void parsesStatusHeadersAndBody() throws IOException {
        byte[] raw = "HTTP/1.1 301 Moved Permanently\r\nLocation: https://shop.example.test/\r\nServer: nginx\r\n\r\nbody".getBytes(StandardCharsets.ISO_8859_1);
        Probe.Reply reply = Probe.parse(raw);
        assertEquals(301, reply.status());
        assertEquals("https://shop.example.test/", reply.headers().get("location"));
        assertEquals("body", reply.body());
    }

    @Test
    void refusesWhatIsNotHttp() {
        assertThrows(IOException.class, () -> Probe.parse("garbage".getBytes(StandardCharsets.ISO_8859_1)));
        assertThrows(IOException.class, () -> Probe.parse("SMTP 220 hello\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1)));
    }
}
