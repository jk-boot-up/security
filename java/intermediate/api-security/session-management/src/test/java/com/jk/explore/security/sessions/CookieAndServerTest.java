package com.jk.explore.security.sessions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CookieAndServerTest {

    @Test
    void theSessionCookieHasNoProblems() {
        assertEquals(List.of(), Cookies.problems(Cookies.sessionCookie("x")));
    }

    @Test
    void eachMissingAttributeIsReported() {
        assertEquals(List.of("no Secure", "no HttpOnly", "no SameSite", "no __Host- prefix"), Cookies.problems("SESSIONID=x"));
        assertEquals(List.of("__Host- needs Path=/ and no Domain"),
                Cookies.problems("__Host-session=x; Path=/; Domain=bank.example; Secure; HttpOnly; SameSite=Lax"));
        assertEquals(List.of("no SameSite"), Cookies.problems("__Host-session=x; Path=/; Secure; HttpOnly; SameSite=None"));
    }

    @Test
    void theBrowserForgetsACookieWithMaxAgeZero() {
        Browser browser = new Browser();
        browser.receive(Cookies.sessionCookie("abc"));
        assertEquals("abc", browser.get(Cookies.NAME).orElseThrow());
        browser.receive(Cookies.forget());
        assertTrue(browser.get(Cookies.NAME).isEmpty());
    }

    @Test
    void theRealServerSignsInReadsAndSignsOut() throws Exception {
        SessionManager bank = new SessionManager(new SimClock(), Settings.bank(), new SessionStore(), SessionDemo.users());
        try (BankServer server = new BankServer(bank); BankClient client = new BankClient(server.port())) {
            assertEquals(401, client.account().status());
            assertEquals(401, client.signIn(SessionDemo.ANA, "wrong").status());
            BankClient.Reply in = client.signIn(SessionDemo.ANA, SessionDemo.PASSWORD);
            assertEquals(200, in.status());
            assertEquals(List.of(), Cookies.problems(in.setCookie()));
            assertEquals(200, client.account().status());
            String copy = client.browser.get(Cookies.NAME).orElseThrow();
            assertEquals(200, client.signOut().status());
            client.browser.put(Cookies.NAME, copy);
            assertEquals(401, client.account().status());
        }
    }

    @Test
    void theRealServerIssuesANewIdAtSignIn() throws Exception {
        SessionManager bank = new SessionManager(new SimClock(), Settings.bank(), new SessionStore(), SessionDemo.users());
        String planted = bank.start();
        try (BankServer server = new BankServer(bank); BankClient client = new BankClient(server.port())) {
            client.browser.put(Cookies.NAME, planted);
            client.signIn(SessionDemo.ANA, SessionDemo.PASSWORD);
            assertTrue(!planted.equals(client.browser.get(Cookies.NAME).orElseThrow()));
            assertEquals(Answer.UNKNOWN, bank.account(planted));
        }
    }
}
