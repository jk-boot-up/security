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

    private static byte[] syn() {
        TcpSegment segment = new TcpSegment(51000, 443, 1000, 0, TcpSegment.SYN, 65535, new byte[0]);
        return segment.encode(CUSTOMER, SHOP);
    }

    private static void act1Layers(StringBuilder out) throws BadPacketException {
        line(out, "ONE. Layers: a packet inside a packet.");
        byte[] tcp = syn();
        byte[] ip = new Ipv4Packet(CUSTOMER, SHOP, 64, tcp).encode();
        line(out, "  a customer's browser sends a SYN from " + Address.text(CUSTOMER) + " to the shop " + Address.text(SHOP) + " port 443");
        line(out, "  the TCP segment is " + tcp.length + " bytes, and inside an IP packet it is " + ip.length + " bytes");
        Ipv4Packet packet = Ipv4Packet.decode(ip);
        TcpSegment segment = TcpSegment.decode(packet.source(), packet.destination(), packet.payload());
        line(out, "  decoded again: from " + Address.text(packet.source()) + " to " + Address.text(packet.destination())
                + ", ports " + segment.sourcePort() + " to " + segment.destinationPort() + ", SYN flag: " + segment.has(TcpSegment.SYN));
        line(out, "  nothing in either header says who the sender really is: the source address is just a field");
    }

    private static void act2ChecksumIsNotSecurity(StringBuilder out) throws BadPacketException {
        line(out, "TWO. A checksum catches accidents, not attackers.");
        byte[] ip = new Ipv4Packet(CUSTOMER, SHOP, 64, syn()).encode();
        byte[] damaged = ip.clone();
        damaged[8] = 1;
        line(out, "  one byte changed in transit (the time to live): " + refusal(damaged));
        Ipv4Packet original = Ipv4Packet.decode(ip);
        byte[] forged = new Ipv4Packet(original.source(), original.destination(), 1, original.payload()).encode();
        line(out, "  the same change made by someone who recomputes the checksum: " + refusal(forged));
        line(out, "  the checksum has no key, so anyone can make it match: it is not protection against an attacker");
    }

    private static String refusal(byte[] bytes) {
        try {
            Ipv4Packet.decode(bytes);
            return "accepted";
        } catch (BadPacketException e) {
            return "refused (" + e.getMessage() + ")";
        }
    }

    private static void act3Handshake(StringBuilder out) {
        line(out, "THREE. The three-way handshake.");
        Listener shop = new Listener(8, false, SECRET, SequenceNumbers.keyed(SECRET));
        Optional<Long> synAck = shop.onSyn(CUSTOMER, 51000, 1000);
        line(out, "  the customer sends SYN with sequence number 1000");
        line(out, "  the shop answers SYN-ACK, acknowledging 1001, and now holds a half-open entry: " + shop.halfOpenCount());
        boolean done = shop.onAck(CUSTOMER, 51000, 1001, synAck.orElseThrow() + 1);
        line(out, "  the customer answers ACK: connection established: " + done + ", half-open entries left: " + shop.halfOpenCount());
        boolean wrong = new Listener(8, false, SECRET, SequenceNumbers.keyed(SECRET)).onAck(CUSTOMER, 51000, 1001, 5);
        line(out, "  an ACK with no SYN before it: established: " + wrong);
    }

    private static void act4SynFlood(StringBuilder out) {
        line(out, "FOUR. A SYN flood.");
        Listener plain = new Listener(8, false, SECRET, SequenceNumbers.keyed(SECRET));
        for (int i = 0; i < 8; i++) {
            plain.onSyn(Address.parse("100.64.0." + (i + 1)), 40000 + i, 7);
        }
        line(out, "  8 SYNs from addresses that never answer fill the shop's table of 8 half-open entries: " + plain.halfOpenCount());
        Optional<Long> real = plain.onSyn(CUSTOMER, 51000, 1000);
        line(out, "  a real customer's SYN is now answered: " + real.isPresent() + "  <- THE SHOP IS DENYING SERVICE TO ITS CUSTOMERS");
        Listener cookies = new Listener(8, true, SECRET, SequenceNumbers.keyed(SECRET));
        for (int i = 0; i < 8; i++) {
            cookies.onSyn(Address.parse("100.64.0." + (i + 1)), 40000 + i, 7);
        }
        Optional<Long> answered = cookies.onSyn(CUSTOMER, 51000, 1000);
        boolean done = cookies.onAck(CUSTOMER, 51000, 1001, answered.orElseThrow() + 1);
        line(out, "  with SYN cookies the shop keeps no entry: half-open entries " + cookies.halfOpenCount()
                + ", and the real customer connects: " + done);
        line(out, "  the cost: cookies carry no options and take work for every SYN, so they are a defence for when the table is full");
    }

    private static void act5GuessableNumbers(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FIVE. Guessable numbers, and a stream anyone can read.");
        out.append(forgery("a counter that adds 64000", SequenceNumbers.counter(1000, 64000)));
        out.append(forgery("a keyed hash", SequenceNumbers.keyed(SECRET)));
        String card = "card=4111111111111111&order=A-1001";
        PlaintextTap.Result result = PlaintextTap.send(card);
        line(out, "  a real TCP connection on this machine: connected " + result.connected() + ", the server received every byte: " + result.serverReceivedAll());
        line(out, "  what a sniffer on the wire sees contains the card number: " + result.seenOnTheWire().contains("4111111111111111")
                + "  <- TCP DOES NOT HIDE ANYTHING");
        line(out, "  TCP gives reliable, ordered delivery. Secrecy and identity come from TLS, which is the transport-security module");
    }

    private static String forgery(String how, SequenceNumbers numbers) {
        Listener shop = new Listener(8, false, SECRET, numbers);
        long seen = shop.onSyn(ATTACKER, 50000, 1).orElseThrow();
        shop.onSyn(TRUSTED_ADMIN, 22222, 5000);
        long guess = (seen + 64000) & 0xffffffffL;
        boolean in = shop.onAck(TRUSTED_ADMIN, 22222, 5001, guess + 1);
        StringBuilder out = new StringBuilder();
        line(out, "  the shop picks its numbers with " + how + ": the attacker, who sends a SYN pretending to be the admin machine "
                + Address.text(TRUSTED_ADMIN) + ", guesses the next number from its own connection and is accepted as the admin: "
                + in + (in ? "  <- A FORGED CONNECTION" : ""));
        return out.toString();
    }
}
