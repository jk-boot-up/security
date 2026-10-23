package com.jk.explore.security.dast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class BankAppTest {

    private static HttpResponse<String> get(BankApp app, String path, String cookie) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path));
        if (cookie != null) {
            b.header("Cookie", "session=" + cookie);
        }
        return HttpClient.newHttpClient().send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void theFirstVersionReflectsScriptAndTheFixedOneEscapesIt() throws Exception {
        try (BankApp a = new BankApp(false); BankApp b = new BankApp(true)) {
            assertTrue(get(a, "/search?q=%3Cscript%3E", null).body().contains("<script>"));
            assertFalse(get(b, "/search?q=%3Cscript%3E", null).body().contains("<script>"));
            assertTrue(get(b, "/search?q=%3Cscript%3E", null).body().contains("&lt;script&gt;"));
        }
    }

    @Test
    void theFirstVersionAnswersAQueryWithAllRowsAndTheFixedOneRefuses() throws Exception {
        try (BankApp a = new BankApp(false); BankApp b = new BankApp(true)) {
            String all = get(a, "/account?id=1%20OR%201=1", null).body();
            assertTrue(all.contains("Asha") && all.contains("Ben"));
            assertEquals(400, get(b, "/account?id=1%20OR%201=1", null).statusCode());
            assertTrue(get(b, "/account?id=2", null).body().contains("Ben"));
        }
    }

    @Test
    void theAdminAreaNeedsTheSessionCookie() throws Exception {
        try (BankApp a = new BankApp(false)) {
            assertEquals(302, get(a, "/admin", null).statusCode());
            assertEquals(200, get(a, "/admin", a.sessionToken()).statusCode());
            assertTrue(get(a, "/admin/report?title=x", a.sessionToken()).body().contains("<h2>x</h2>"));
        }
    }

    @Test
    void theFixedVersionSendsSecurityHeaders() throws Exception {
        try (BankApp a = new BankApp(false); BankApp b = new BankApp(true)) {
            assertTrue(get(a, "/", null).headers().firstValue("Content-Security-Policy").isEmpty());
            assertTrue(get(b, "/", null).headers().firstValue("Content-Security-Policy").isPresent());
            assertEquals("nosniff", get(b, "/", null).headers().firstValue("X-Content-Type-Options").orElseThrow());
        }
    }

    @Test
    void theLoginCookieHasFlagsOnlyInTheFixedVersion() throws Exception {
        try (BankApp a = new BankApp(false); BankApp b = new BankApp(true)) {
            for (BankApp app : new BankApp[] {a, b}) {
                HttpResponse<String> r = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + "/login"))
                        .POST(HttpRequest.BodyPublishers.ofString("user=a")).build(), HttpResponse.BodyHandlers.ofString());
                String cookie = r.headers().firstValue("Set-Cookie").orElseThrow();
                assertEquals(app.fixed(), cookie.contains("HttpOnly") && cookie.contains("SameSite=Strict"));
            }
        }
    }
}
