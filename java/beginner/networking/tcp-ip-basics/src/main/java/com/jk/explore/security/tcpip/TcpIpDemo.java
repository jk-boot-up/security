package com.jk.explore.security.tcpip;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. */
public class TcpIpDemo {

    static final int SHOP = Address.parse("203.0.113.10");
    static final int CUSTOMER = Address.parse("198.51.100.7");
    static final int TRUSTED_ADMIN = Address.parse("10.0.0.5");
    static final int ATTACKER = Address.parse("192.0.2.66");
    static final byte[] SECRET = "demo-secret-for-sequence-numbers".getBytes(StandardCharsets.UTF_8);

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException, BadPacketException {
        StringBuilder out = new StringBuilder();
        act1Layers(out);
        act2ChecksumIsNotSecurity(out);
        act3Handshake(out);
        act4SynFlood(out);
        act5GuessableNumbers(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** Bytes as hex, two bytes (one 16-bit word) per group: "4500 0028 ...". */
    static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder();
        for (int word : Checksum.words(bytes)) {
            text.append(text.isEmpty() ? "" : " ").append(String.format("%04x", word));
        }
        return text.toString();
    }

    private static byte[] syn() {
        TcpSegment segment = new TcpSegment(51000, 443, 1000, 0, TcpSegment.SYN, 65535, new byte[0]);
        return segment.encode(CUSTOMER, SHOP);
    }

    private static void act1Layers(StringBuilder out) throws BadPacketException {
        line(out, "ONE. Layers: a packet inside a packet.");
        byte[] tcp = syn();
        byte[] ip = new Ipv4Packet(CUSTOMER, SHOP, 64, tcp).encode();
        line(out, "  THE PACKET SENT (the customer's browser opens a connection):");
        line(out, "    TCP segment  " + tcp.length + " bytes  ports 51000 -> 443, SYN, seq 1000");
        line(out, "    IP header    20 bytes  " + Address.text(CUSTOMER) + " -> " + Address.text(SHOP) + ", TTL 64");
        line(out, "    IP packet    " + ip.length + " bytes  = IP header + TCP segment");
        line(out, "    ip   " + hex(Arrays.copyOf(ip, Ipv4Packet.HEADER)));
        line(out, "    tcp  " + hex(tcp));
        Ipv4Packet packet = Ipv4Packet.decode(ip);
        TcpSegment segment = TcpSegment.decode(packet.source(), packet.destination(), packet.payload());
        line(out, "  THE PACKET RECEIVED (the shop decodes the 40 bytes):");
        line(out, "    from  " + Address.text(packet.source()) + " port " + segment.sourcePort());
        line(out, "    to    " + Address.text(packet.destination()) + " port " + segment.destinationPort());
        line(out, "    SYN flag " + segment.has(TcpSegment.SYN) + ", sequence number " + segment.sequence());
        line(out, "  the source address is just a field  <- ANYONE CAN WRITE IT");
    }

    private static void act2ChecksumIsNotSecurity(StringBuilder out) throws BadPacketException {
        line(out, "TWO. A checksum catches accidents, not attackers.");
        byte[] ip = new Ipv4Packet(CUSTOMER, SHOP, 64, syn()).encode();
        byte[] header = Arrays.copyOf(ip, Ipv4Packet.HEADER);
        header[10] = 0;
        header[11] = 0;
        long total = Checksum.total(header);
        line(out, "  HOW THE CHECKSUM IS MADE (IP header, checksum field 0000):");
        line(out, "    words  " + hex(header));
        line(out, String.format("    sum    %x (hex)", total));
        line(out, String.format("    fold   %04x + %x = %04x", total & 0xffff, total >> 16, Checksum.sum(header)));
        line(out, String.format("    flip   ffff - %04x = %04x  <- the checksum", Checksum.sum(header), Checksum.of(header)));
        line(out, "  BEFORE THE CHANGE (as the customer sent it):");
        line(out, String.format("    time to live 64, checksum %04x: %s", checksumOf(ip), verdict(ip)));
        byte[] damaged = ip.clone();
        damaged[8] = 1;
        Ipv4Packet original = Ipv4Packet.decode(ip);
        byte[] forged = new Ipv4Packet(original.source(), original.destination(), 1, original.payload()).encode();
        line(out, "  AFTER THE CHANGE (time to live 64 -> 1 on the way):");
        line(out, String.format("    by accident, checksum kept %04x: %s", checksumOf(damaged), verdict(damaged)));
        line(out, String.format("    by an attacker, new checksum %04x: %s  <- NOT CAUGHT", checksumOf(forged), verdict(forged)));
        line(out, "  the checksum has no key, so anyone can make it match");
    }

    private static int checksumOf(byte[] packet) {
        return ((packet[10] & 0xff) << 8) | (packet[11] & 0xff);
    }

    private static String verdict(byte[] bytes) {
        try {
            Ipv4Packet.decode(bytes);
            return "accepted";
        } catch (BadPacketException e) {
            return "refused";
        }
    }

    private static void act3Handshake(StringBuilder out) {
        line(out, "THREE. The three-way handshake.");
        Listener shop = new Listener(8, false, SECRET, SequenceNumbers.keyed(SECRET));
        long server = shop.onSyn(CUSTOMER, 51000, 1000).orElseThrow();
        line(out, "  THE REQUEST (customer -> shop):");
        line(out, "    1. SYN      seq 1000");
        line(out, "  THE RESPONSE (shop -> customer):");
        line(out, "    2. SYN-ACK  seq " + server + ", ack 1001 (= 1000 + 1)");
        line(out, "       half-open entries in the shop's table: " + shop.halfOpenCount());
        line(out, "  THE REQUEST (customer -> shop):");
        line(out, "    3. ACK      seq 1001, ack " + (server + 1) + " (= " + server + " + 1)");
        boolean done = shop.onAck(CUSTOMER, 51000, 1001, server + 1);
        line(out, "  THE RESULT:");
        line(out, "    established: " + done + ", half-open entries left: " + shop.halfOpenCount());
        boolean wrong = new Listener(8, false, SECRET, SequenceNumbers.keyed(SECRET)).onAck(CUSTOMER, 51000, 1001, 5);
        line(out, "    an ACK with no SYN before it: established: " + wrong);
    }

    private static void act4SynFlood(StringBuilder out) {
        line(out, "FOUR. A SYN flood.");
        Listener plain = new Listener(8, false, SECRET, SequenceNumbers.keyed(SECRET));
        flood(plain);
        line(out, "  BEFORE THE FIX (a table of 8 half-open entries):");
        line(out, "    8 SYNs from 100.64.0.1 to 100.64.0.8, never answered");
        line(out, "    half-open entries: " + plain.halfOpenCount() + " of 8, the table is full");
        Optional<Long> real = plain.onSyn(CUSTOMER, 51000, 1000);
        line(out, "    a real customer's SYN answered: " + real.isPresent() + "  <- DENIAL OF SERVICE");
        Listener cookies = new Listener(8, true, SECRET, SequenceNumbers.keyed(SECRET));
        flood(cookies);
        long cookie = cookies.onSyn(CUSTOMER, 51000, 1000).orElseThrow();
        line(out, "  AFTER THE FIX (SYN cookies):");
        line(out, "    the same 8 SYNs: half-open entries " + cookies.halfOpenCount());
        line(out, "    cookie = HMAC(secret, 198.51.100.7, 51000, 1000)");
        line(out, "           = " + cookie + ", sent as the SYN-ACK's seq");
        boolean done = cookies.onAck(CUSTOMER, 51000, 1001, cookie + 1);
        line(out, "    the ACK brings " + (cookie + 1) + " = cookie + 1: connects " + done);
        line(out, "  the cost: few TCP options, and a hash for every SYN");
    }

    private static void flood(Listener listener) {
        for (int i = 0; i < 8; i++) {
            listener.onSyn(Address.parse("100.64.0." + (i + 1)), 40000 + i, 7);
        }
    }

    private static void act5GuessableNumbers(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. Guessable numbers, and a stream anyone can read.");
        forgery(out, "BEFORE THE FIX (a counter that adds 64000):", SequenceNumbers.counter(1000, 64000));
        forgery(out, "AFTER THE FIX (a keyed hash, RFC 6528):", SequenceNumbers.keyed(SECRET));
        String card = "card=4111111111111111&order=A-1001";
        PlaintextTap.Result result = PlaintextTap.send(card);
        line(out, "  THE STREAM SENT (a real connection on this machine):");
        line(out, "    " + card);
        line(out, "    connected " + result.connected() + ", the shop received every byte: " + result.serverReceivedAll());
        line(out, "  WHAT A SNIFFER ON THE WIRE SEES:");
        line(out, "    " + result.seenOnTheWire());
        line(out, "    contains the card number: " + result.seenOnTheWire().contains("4111111111111111")
                + "  <- TCP HIDES NOTHING");
        line(out, "  secrecy and identity come from TLS");
    }

    private static void forgery(StringBuilder out, String title, SequenceNumbers numbers) {
        Listener shop = new Listener(8, false, SECRET, numbers);
        long seen = shop.onSyn(ATTACKER, 50000, 1).orElseThrow();
        long sent = shop.onSyn(TRUSTED_ADMIN, 22222, 5000).orElseThrow();
        long guess = (seen + 64000) & 0xffffffffL;
        boolean in = shop.onAck(TRUSTED_ADMIN, 22222, 5001, guess + 1);
        line(out, "  " + title);
        line(out, String.format("    %-36s%d", "the attacker's own SYN-ACK seq:", seen));
        line(out, String.format("    %-36s%d", "its guess for the admin (+ 64000):", guess));
        line(out, String.format("    %-36s%d", "what the shop really sent 10.0.0.5:", sent));
        line(out, "    forged ACK accepted: " + in + (in ? "  <- A FORGED CONNECTION" : ""));
    }
}
