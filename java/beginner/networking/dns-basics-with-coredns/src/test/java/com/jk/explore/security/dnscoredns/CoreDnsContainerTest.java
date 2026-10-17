package com.jk.explore.security.dnscoredns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** These tests start real CoreDNS containers, so they need a running Docker: run them with ./gradlew integrationTest. */
@Tag("docker")
class CoreDnsContainerTest {

    private CoreDns good() throws Exception {
        return CoreDns.start(ShopZone.corefile(), ShopZone.zoneFile(ShopZone.GOOD_ADDRESS, ShopZone.GOOD_TTL));
    }

    @Test
    void answersTheRecordOverUdpAndTcp() throws Exception {
        try (CoreDns server = good()) {
            DnsMessage udp = Client.udp(server.udpPort(), DnsMessage.query(1, "www.shop.example")).message();
            DnsMessage tcp = Client.tcp(server.tcpPort(), DnsMessage.query(2, "www.shop.example")).message();
            assertEquals("203.0.113.10", Address.text(udp.answers().get(0).address()));
            assertEquals("203.0.113.10", Address.text(tcp.answers().get(0).address()));
            assertEquals(1, udp.id());
            assertEquals(2, tcp.id());
        }
    }

    @Test
    void unknownNameIsNxdomainAndOtherZonesAreRefused() throws Exception {
        try (CoreDns server = good()) {
            assertEquals(3, Client.udp(server.udpPort(), DnsMessage.query(3, "nope.shop.example")).message().rcode());
            assertEquals(5, Client.udp(server.udpPort(), DnsMessage.query(4, "www.other.example")).message().rcode());
        }
    }

    @Test
    void rewriteAnswersUnderTheNameAsked() throws Exception {
        try (CoreDns server = good()) {
            DnsMessage message = Client.udp(server.udpPort(), DnsMessage.query(5, "legacy.shop.example")).message();
            assertEquals("legacy.shop.example", message.answers().get(0).name());
            assertEquals("203.0.113.10", Address.text(message.answers().get(0).address()));
        }
    }

    @Test
    void cachedAnswersCountDown() throws Exception {
        try (CoreDns server = good()) {
            Client.udp(server.udpPort(), DnsMessage.query(6, "www.shop.example"));
            Thread.sleep(1200);
            long a = Client.udp(server.udpPort(), DnsMessage.query(7, "www.shop.example")).message().answers().get(0).ttl();
            Thread.sleep(1200);
            long b = Client.udp(server.udpPort(), DnsMessage.query(8, "www.shop.example")).message().answers().get(0).ttl();
            assertTrue(a <= ShopZone.CACHE_SECONDS);
            assertTrue(b < a);
        }
    }

    @Test
    void aWrongRecordIsServedLikeAnyOther() throws Exception {
        try (CoreDns server = CoreDns.start(ShopZone.corefile(), ShopZone.zoneFile(ShopZone.WRONG_ADDRESS, ShopZone.WRONG_TTL))) {
            DnsMessage.Answer answer = Client.udp(server.udpPort(), DnsMessage.query(9, "www.shop.example")).message().answers().get(0);
            assertEquals("192.0.2.66", Address.text(answer.address()));
        }
    }

    @Test
    void containerIsRemovedWhenClosed() throws Exception {
        String name;
        try (CoreDns server = good()) {
            name = new ProcessBuilder("docker", "ps", "--filter", "name=dns-coredns-", "--format", "{{.Names}}").start()
                    .inputReader().readLine();
            assertTrue(name != null && name.startsWith("dns-coredns-"));
        }
        String after = new ProcessBuilder("docker", "ps", "-a", "--filter", "name=" + name, "--format", "{{.Names}}").start()
                .inputReader().readLine();
        assertEquals(null, after);
    }
}
