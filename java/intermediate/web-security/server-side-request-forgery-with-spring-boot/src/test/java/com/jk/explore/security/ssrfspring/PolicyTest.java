package com.jk.explore.security.ssrfspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** No server needed: how addresses are judged and how an address is taken apart. */
class PolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "127.1", "169.254.169.254", "2852039166", "10.0.0.5", "172.16.4.1", "192.168.1.1", "0.0.0.0", "100.64.0.1",
            "::1", "fd00::1", "fe80::1", "::ffff:a9fe:a9fe", "::ffff:127.0.0.1", "224.0.0.1", "255.255.255.255"})
    void privateAndSpecialAddressesAreRefusedHoweverTheyAreSpelled(String spelling) throws Exception {
        assertFalse(AddressPolicy.isPublic(InetAddress.getByName(spelling)), spelling);
    }

    @ParameterizedTest
    @ValueSource(strings = {"203.0.113.10", "93.184.216.34", "8.8.8.8", "2606:4700:4700::1111"})
    void publicAddressesAreAllowed(String spelling) throws Exception {
        assertTrue(AddressPolicy.isPublic(InetAddress.getByName(spelling)), spelling);
    }

    @Test
    void theDecimalSpellingIsJustAHostStringUntilItIsResolved() throws Exception {
        assertEquals("2852039166", java.net.URI.create("http://2852039166/").getHost());
        assertEquals("169.254.169.254", InetAddress.getByName(Target.parse("http://2852039166/").host()).getHostAddress());
    }

    @Test
    void anAddressIsTakenApartTheSameWayEveryTime() throws Exception {
        Target t = Target.parse("HTTP://user:pw@Images.Example:8080/a/b?x=1");
        assertEquals("http", t.scheme());
        assertEquals("user:pw", t.userInfo());
        assertEquals("Images.Example", t.host());
        assertEquals(8080, t.port());
        assertEquals("/a/b?x=1", t.path());
        assertEquals("::ffff:a9fe:a9fe", Target.parse("http://[::ffff:a9fe:a9fe]/x").host());
        assertEquals("/", Target.parse("http://images.example").path());
        assertEquals("/?q=1", Target.parse("http://images.example?q=1").path());
        assertEquals(443, Target.parse("https://images.example/").port());
    }

    @Test
    void otherSchemesAndEmptyHostsAreRefused() {
        assertThrows(ImportRefused.class, () -> Target.parse("file:///etc/passwd"));
        assertThrows(ImportRefused.class, () -> Target.parse("gopher://images.example/"));
        assertThrows(ImportRefused.class, () -> Target.parse("http:///x"));
        assertThrows(ImportRefused.class, () -> Target.parse(null));
    }

    @Test
    void aRedirectIsReadAsANewAddress() throws Exception {
        Target t = Target.parse("http://images.example/a");
        assertEquals("http://other.example/x", t.redirectTo("http://other.example/x"));
        assertEquals("http://images.example:80/x", t.redirectTo("/x"));
    }
}
