package com.jk.explore.security.ssrf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
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
            if (attack.equals(SsrfDemo.REBINDING)) {
                continue; // fetches the attacker's own image: covered below
            }
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
        assertEquals("PNG-attacker-cat", new SafeImporter(dns, net).importImage(SsrfDemo.REBINDING));
    }

    @Test
    void checkThenConnectIsFooledByRebinding() {
        String result = SsrfDemo.checkThenFetch(Network.dns(), Network.world());
        assertTrue(result.contains("checked as public=true"));
        assertTrue(result.contains("internal admin panel"));
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
    void urlParsingFindsTheRealHost() {
        Url u = Url.parse("http://images.example@10.0.0.5:8080/admin?x=1");
        assertEquals("10.0.0.5", u.host());
        assertEquals("images.example", u.userInfo());
        assertEquals(8080, u.port());
        assertEquals("[::1]", "[" + Url.parse("http://[::1]:80/").host() + "]");
    }
}
