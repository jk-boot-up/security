package com.jk.explore.security.dnscoredns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.jupiter.api.Test;

/** Tests that need no Docker: the text of the files, and the client against small servers written here. */
class ConfigAndClientTest {

    private static final InetAddress LOCAL = InetAddress.getLoopbackAddress();

    @Test
    void corefileNamesTheZoneAndTheThreePlugins() {
        String corefile = ShopZone.corefile();
        assertTrue(corefile.startsWith("shop.example:53 {"));
        assertTrue(corefile.contains("file /etc/coredns/shop.example.db"));
        assertTrue(corefile.contains("rewrite name exact legacy.shop.example. www.shop.example."));
        assertTrue(corefile.contains("cache 60"));
    }

    @Test
    void zoneFileHasTheRecordAndTheRequiredStartOfAuthority() {
        String zone = ShopZone.zoneFile("203.0.113.10", 300);
        assertTrue(zone.contains("$ORIGIN shop.example."));
        assertTrue(zone.contains("IN SOA"));
        assertTrue(zone.contains("www 300 IN A 203.0.113.10"));
        assertFalse(zone.contains("192.0.2.66"));
        assertTrue(ShopZone.zoneFile("192.0.2.66", 86400).contains("www 86400 IN A 192.0.2.66"));
    }

    @Test
    void imageIsPinnedToAnExactVersion() {
        assertEquals("coredns/coredns:1.14.7", CoreDns.IMAGE);
    }

    @Test
    void udpClientReadsAnAnswerFromASmallServer() throws Exception {
        try (DatagramSocket server = new DatagramSocket(0, LOCAL)) {
            Thread thread = Thread.ofPlatform().start(() -> {
                try {
                    DatagramPacket in = new DatagramPacket(new byte[512], 512);
                    server.receive(in);
                    byte[] reply = DnsMessage.decode(java.util.Arrays.copyOf(in.getData(), in.getLength()))
                            .answeredWith(300, Address.parse("203.0.113.10")).encode();
                    server.send(new DatagramPacket(reply, reply.length, in.getAddress(), in.getPort()));
                } catch (Exception e) {
                    // the client times out and the test fails
                }
            });
            Client.Reply reply = Client.udp(server.getLocalPort(), DnsMessage.query(0x1234, "www.shop.example"));
            thread.join();
            assertEquals(0x1234, reply.message().id());
            assertEquals("203.0.113.10", Address.text(reply.message().answers().get(0).address()));
        }
    }

    @Test
    void tcpClientSendsAndExpectsATwoByteLength() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, LOCAL)) {
            int[] seenLength = new int[1];
            Thread thread = Thread.ofPlatform().start(() -> {
                try (Socket socket = server.accept()) {
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    seenLength[0] = in.readUnsignedShort();
                    byte[] query = new byte[seenLength[0]];
                    in.readFully(query);
                    byte[] reply = DnsMessage.decode(query).answeredWith(300, Address.parse("203.0.113.10")).encode();
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeShort(reply.length);
                    out.write(reply);
                    out.flush();
                } catch (Exception e) {
                    // the client fails and the test fails
                }
            });
            byte[] expected = DnsMessage.query(7, "www.shop.example").encode();
            Client.Reply reply = Client.tcp(server.getLocalPort(), DnsMessage.query(7, "www.shop.example"));
            thread.join();
            assertEquals(expected.length, seenLength[0]);
            assertEquals(reply.message().id(), 7);
            assertEquals(DnsMessage.query(7, "www.shop.example").answeredWith(300, 1).encode().length + 2, reply.bytes());
        }
    }

    @Test
    void nothingListeningMeansNoAnswer() throws Exception {
        int free;
        try (ServerSocket socket = new ServerSocket(0, 1, LOCAL)) {
            free = socket.getLocalPort();
        }
        assertFalse(Client.tcpAnswers(free));
    }
}
