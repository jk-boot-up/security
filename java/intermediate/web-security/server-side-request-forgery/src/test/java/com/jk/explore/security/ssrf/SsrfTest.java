package com.jk.explore.security.ssrf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.Test;

class SsrfTest {

    private final Dns dns = Network.dns();
    private final Network net = Network.world();

    @Test
    void naiveImporterReachesTheMetadataServiceAndInternalPages() {
        Importer naive = new NaiveImporter(dns, net);
        assertEquals(Network.DEMO_SECRET, naive.importImage(SsrfDemo.METADATA));
        assertEquals("internal admin panel: 3 customers", naive.importImage(SsrfDemo.INTERNAL));
        assertEquals("DB_PASSWORD=demo-not-real", naive.importImage(SsrfDemo.LOCALHOST));
    }

    @Test
    void blocklistStopsTheExamplesAndNothingElse() {
        Importer b = new BlocklistImporter(dns, net);
        assertThrows(ImportRefused.class, () -> b.importImage(SsrfDemo.METADATA));
        assertThrows(ImportRefused.class, () -> b.importImage(SsrfDemo.LOCALHOST));
        for (String bypass : new String[] {SsrfDemo.DECIMAL, SsrfDemo.SHORT_LOOPBACK, SsrfDemo.MAPPED, SsrfDemo.DNS_NAME, SsrfDemo.REDIRECT}) {
            assertFalse(b.importImage(bypass).isEmpty(), bypass);
        }
    }

    @Test
    void safeImporterRefusesEveryAttack() {
        Importer safe = new SafeImporter(dns, net);
        for (String attack : SsrfDemo.attacks()) {
            assertThrows(ImportRefused.class, () -> safe.importImage(attack), attack);
        }
    }

    @Test
    void safeImporterStillImportsHonestImages() {
        assertEquals("PNG-cat", new SafeImporter(dns, net).importImage(SsrfDemo.HONEST));
    }

    @Test
    void aRedirectToAPublicAddressIsFollowedButARedirectInsideIsNot() {
        Importer safe = new SafeImporter(dns, net);
        assertEquals("PNG-cat", safe.importImage("http://attacker.example/to-public"));
        assertThrows(ImportRefused.class, () -> safe.importImage("http://attacker.example/go"));
    }

    @Test
    void rebindingCannotChangeTheCheckedAddress() {
        assertEquals("PNG-attacker-cat", new SafeImporter(dns, net).importImage("http://rebind.attacker.example/cat.png"));
        SafeImporter safe = new SafeImporter(Network.dns(), net);
        assertThrows(ImportRefused.class, () -> safe.importImage(SsrfDemo.REBINDING));
        assertEquals("203.0.113.9 is public: connect to it", safe.trace().get(2));
        assertEquals("refused: the server answered 404", safe.trace().get(3));
    }

    @Test
    void theTraceRecordsEachCheckOfARedirect() {
        SafeImporter safe = new SafeImporter(dns, net);
        assertThrows(ImportRefused.class, () -> safe.importImage(SsrfDemo.REDIRECT));
        assertEquals(List.of("scheme http, port 80, no user info: allowed", "look up attacker.example once: [203.0.113.9]",
                "203.0.113.9 is public: connect to it", "302 redirect to 169.254.169.254: check it again",
                "scheme http, port 80, no user info: allowed", "look up 169.254.169.254 once: [169.254.169.254]",
                "refused: 169.254.169.254 is link-local"), safe.trace());
    }

    @Test
    void checkThenConnectIsFooledByRebinding() {
        SsrfDemo.CheckThenConnect result = SsrfDemo.checkThenFetch(Network.dns(), Network.world());
        assertEquals("203.0.113.9", result.checked());
        assertTrue(result.looksPublic());
        assertEquals("10.0.0.5", result.connected());
        assertEquals("internal admin panel: 3 customers", result.fetched());
    }

    @Test
    void oneBadAddressAmongManyRefusesTheName() {
        assertThrows(ImportRefused.class, () -> new SafeImporter(dns, net).importImage("http://dual.attacker.example/admin"));
    }

    @Test
    void onlyHttpOnWebPortsIsAllowedAndResponsesAreLimited() {
        Importer safe = new SafeImporter(dns, net);
        assertThrows(ImportRefused.class, () -> safe.importImage("file:///etc/passwd"));
        assertThrows(ImportRefused.class, () -> safe.importImage("ftp://images.example/cat.png"));
        assertThrows(ImportRefused.class, () -> safe.importImage("http://images.example:6379/cat.png"));
        assertThrows(ImportRefused.class, () -> safe.importImage("http://images.example/report.html"));
        assertThrows(ImportRefused.class, () -> safe.importImage("http://images.example/huge.png"));
        assertThrows(ImportRefused.class, () -> safe.importImage("not a url"));
    }

    @Test
    void addressPolicyReadsTheAddressNotTheSpelling() throws Exception {
        for (String inside : new String[] {"127.0.0.1", "127.1", "2130706433", "10.1.2.3", "172.16.0.1", "192.168.1.1", "169.254.169.254",
                "100.64.0.1", "0.0.0.0", "::1", "fe80::1", "fd00::1", "::ffff:7f00:1", "224.0.0.1"}) {
            assertFalse(AddressPolicy.isPublic(InetAddress.getByName(inside)), inside);
        }
        for (String outside : new String[] {"93.184.216.34", "8.8.8.8", "203.0.113.9", "2606:4700:4700::1111"}) {
            assertTrue(AddressPolicy.isPublic(InetAddress.getByName(outside)), outside);
        }
    }

    @Test
    void addressPolicyNamesTheKindOfEachAddress() throws Exception {
        assertEquals("loopback", AddressPolicy.kind(InetAddress.getByName("127.1")));
        assertEquals("link-local", AddressPolicy.kind(InetAddress.getByName("2852039166")));
        assertEquals("link-local", AddressPolicy.kind(InetAddress.getByName("::ffff:a9fe:a9fe")));
        assertEquals("private", AddressPolicy.kind(InetAddress.getByName("10.0.0.5")));
        assertEquals("shared", AddressPolicy.kind(InetAddress.getByName("100.64.0.1")));
        assertEquals("unique local", AddressPolicy.kind(InetAddress.getByName("fd00::1")));
        assertEquals("public", AddressPolicy.kind(InetAddress.getByName("93.184.216.34")));
    }

    @Test
    void theWorkedSpellingsGiveTheJdksExactAddress() throws Exception {
        for (long n : new long[] {2852039166L, 2130706433L, 167772165L, 0L, 4294967295L}) {
            assertEquals(InetAddress.getByName(Long.toString(n)).getHostAddress(), Spelling.decimal(n).address(), Long.toString(n));
        }
        for (String s : new String[] {"127.1", "10.5", "127.0.1", "192.168.257"}) {
            assertEquals(InetAddress.getByName(s).getHostAddress(), Spelling.shortForm(s).address(), s);
        }
        for (String s : new String[] {"::ffff:a9fe:a9fe", "::ffff:7f00:0001", "::ffff:0a00:0005"}) {
            assertEquals(InetAddress.getByName(s).getHostAddress(), Spelling.mapped(s).address(), s);
        }
        assertEquals(List.of("2852039166 / 16777216 = 169, remainder 16689662", "16689662 / 65536 = 254, remainder 43518",
                "43518 / 256 = 169, remainder 254"), Spelling.decimal(2852039166L).steps());
    }

    @Test
    void urlParsingFindsTheRealHost() {
        Url u = Url.parse("http://images.example@10.0.0.5:8080/admin?x=1");
        assertEquals("10.0.0.5", u.host());
        assertEquals("images.example", u.userInfo());
        assertEquals(8080, u.port());
        assertEquals("[::1]", "[" + Url.parse("http://[::1]:80/").host() + "]");
    }
}
