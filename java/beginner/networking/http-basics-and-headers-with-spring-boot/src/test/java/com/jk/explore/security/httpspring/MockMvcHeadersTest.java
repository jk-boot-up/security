package com.jk.explore.security.httpspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.ServletException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/** The hardened store's headers, checked through the whole filter chain in memory. */
@SpringBootTest(classes = Shop.Hardened.class, properties = "logging.level.root=OFF")
@AutoConfigureMockMvc
class MockMvcHeadersTest {

    @Autowired
    MockMvc mvc;

    private static Map<String, String> headers(MockHttpServletResponse response) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String name : response.getHeaderNames()) {
            map.put(name, response.getHeader(name));
        }
        return map;
    }

    @Test
    void overHttpsAllFiveHeadersArePresent() throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/products").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'"))
                .andReturn().getResponse();
        assertEquals(5, HeaderAudit.score(HeaderAudit.audit(headers(response))));
        assertEquals("max-age=31536000 ; includeSubDomains", response.getHeader("Strict-Transport-Security"));
    }

    @Test
    void overPlainHttpHstsIsLeftOut() throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/products")).andReturn().getResponse();
        assertNull(response.getHeader("Strict-Transport-Security"));
        assertEquals(4, HeaderAudit.score(HeaderAudit.audit(headers(response))));
    }

    @Test
    void anEchoedLineBreakIsRefusedAndNeverWrittenAsASecondHeader() {
        ServletException refused = assertThrows(ServletException.class,
                () -> mvc.perform(get("/echo").param("name", "shoes\r\nSet-Cookie: evil=1")));
        assertInstanceOf(IllegalArgumentException.class, refused.getCause());
    }
}
