package com.jk.explore.security.tcpipdump;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs the online store story in six numbered acts: record a purchase to a capture file, read it back, find a flood,
 * work out a checksum by hand, find a damaged packet, and see what a capture reveals. Each act prints its input and its
 * result in separate, labelled sections.
 */
public class TcpdumpDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws BadPacketException {
        StringBuilder out = new StringBuilder();
        act1Record(out);
        act2ReadBack(out);
        act3Flood(out);
        act4Checksum(out);
        act5Damaged(out);
        act6WhatItReveals(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Record(StringBuilder out) throws BadPacketException {
        line(out, "ONE. Recording a purchase to a capture file.");
        List<byte[]> purchase = Traffic.purchase();
        line(out, "  BEFORE RECORDING (the packets of one purchase):");
        int bytes = 0;
        for (int i = 0; i < purchase.size(); i++) {
            byte[] raw = purchase.get(i);
            TcpSegment segment = TcpSegment.parse(Ipv4Packet.parseUnchecked(raw).payload());
            int data = segment.payload().length;
            line(out, String.format("    packet %d  %-5s %2d bytes = IP 20 + TCP 20 + data %d",
                    i + 1, "[" + Capture.flags(segment) + "]", raw.length, data));
            bytes += raw.length;
        }
        line(out, "    " + purchase.size() + " packets, " + bytes + " bytes");
        byte[] file = PcapFile.write(purchase);
        line(out, "  AFTER RECORDING (the pcap file):");
        line(out, "    file header     24 bytes");
        line(out, "    record headers  " + purchase.size() + " x 16 = " + purchase.size() * 16 + " bytes");
        line(out, "    packet bytes    " + bytes + " bytes");
        line(out, "    capture.pcap    24 + " + purchase.size() * 16 + " + " + bytes + " = " + file.length + " bytes");
        line(out, "  tcpdump, Wireshark and the Python script all read this file");
    }

    private static void act2ReadBack(StringBuilder out) throws BadPacketException {
        line(out, "TWO. Reading it back, in the style of tcpdump.");
        line(out, "  WHAT TCPDUMP PRINTS (one packet, two lines):");
        for (String text : Capture.lines(PcapFile.write(Traffic.purchase()))) {
            int cut = text.indexOf(": ");
            line(out, "    " + text.substring(0, cut + 1));
            line(out, "       " + text.substring(cut + 2));
        }
        line(out, "  S is SYN, a dot is ACK, P is PSH");
        List<TcpSegment> s = new ArrayList<>();
        for (byte[] raw : Traffic.purchase()) {
            s.add(TcpSegment.parse(Ipv4Packet.parseUnchecked(raw).payload()));
        }
        line(out, "  HOW THE NUMBERS FOLLOW:");
        line(out, "    packet 2  ack " + s.get(1).acknowledgement() + " = seq " + s.get(0).sequence() + " + 1");
        line(out, "    packet 3  ack " + s.get(2).acknowledgement() + " = seq " + s.get(1).sequence() + " + 1");
        line(out, "    packet 5  ack " + s.get(4).acknowledgement() + " = seq " + s.get(3).sequence() + " + "
                + s.get(3).payload().length + " bytes of data");
        line(out, "  a SYN counts as one byte");
    }

    private static void act3Flood(StringBuilder out) throws BadPacketException {
        line(out, "THREE. Finding a SYN flood in a capture.");
        byte[] file = PcapFile.write(Traffic.everything());
        line(out, "  THE CAPTURE (" + Traffic.everything().size() + " packets):");
        line(out, "    the purchase         " + Traffic.purchase().size() + " packets");
        line(out, "    a burst of SYNs      " + Traffic.FLOOD_SOURCES + " packets");
        line(out, "    one more SYN         1 packet");
        line(out, "  THE FINDING (who started a handshake, who finished it):");
        Map<String, Boolean> handshakes = Capture.handshakes(file);
        handshakes.forEach((source, done) -> {
            if (done) {
                line(out, "    finished        " + source);
            }
        });
        List<String> unfinished = new ArrayList<>(Capture.unfinishedHandshakes(file));
        line(out, "    never finished  " + unfinished.size() + " sources");
        for (int i = 0; i < unfinished.size(); i += 3) {
            StringBuilder row = new StringBuilder("     ");
            for (String source : unfinished.subList(i, Math.min(i + 3, unfinished.size()))) {
                row.append(String.format(" %-12s", source));
            }
            line(out, row.toString().stripTrailing());
        }
        line(out, "    the real customer is among them: " + unfinished.contains(Address.text(Traffic.CUSTOMER)));
        line(out, "  a flood is told by handshakes that never finish");
    }

    private static void act4Checksum(StringBuilder out) throws BadPacketException {
        line(out, "FOUR. How a checksum is made, step by step.");
        ChecksumSteps steps = ChecksumSteps.forTcp(Traffic.lateSyn());
        line(out, "  the packet: the SYN from 100.64.0.99, as it was sent");
        line(out, "  STEP 1. SPLIT INTO 16-BIT WORDS (checksum field 0000):");
        line(out, "    pseudo-header  " + hex(steps.pseudoWords()));
        line(out, "    TCP header     " + hex(steps.tcpWords().subList(0, 6)));
        line(out, "                   " + hex(steps.tcpWords().subList(6, steps.tcpWords().size())));
        line(out, "  STEP 2. ADD THEM UP:");
        line(out, String.format("    sum            %05x", steps.total()));
        line(out, "  STEP 3. FOLD THE CARRY BACK IN:");
        line(out, String.format("    %04x + %x = %04x", steps.total() & 0xffff, steps.total() >> 16, steps.folded()));
        line(out, "  STEP 4. FLIP EVERY BIT:");
        line(out, String.format("    checksum       %04x", steps.checksum()));
        line(out, String.format("  the checksum the packet carries: %04x, the same: %s", steps.stored(), steps.matches()));
    }

    private static void act5Damaged(StringBuilder out) throws BadPacketException {
        line(out, "FIVE. Finding a damaged packet.");
        ChecksumSteps sent = ChecksumSteps.forTcp(Traffic.lateSyn());
        ChecksumSteps arrived = ChecksumSteps.forTcp(Traffic.damaged());
        line(out, "  BEFORE THE DAMAGE (the SYN as it was sent):");
        line(out, String.format("    checksum in the packet  %04x", sent.stored()));
        line(out, String.format("    worked out again        %04x  match: %s", sent.checksum(), sent.matches()));
        line(out, "  AFTER THE DAMAGE (the same SYN as it arrived):");
        line(out, String.format("    checksum in the packet  %04x", arrived.stored()));
        line(out, String.format("    worked out again        %04x  match: %s", arrived.checksum(), arrived.matches()));
        line(out, "  packets in the capture whose checksum does not match: "
                + Capture.damagedCount(PcapFile.write(Traffic.everything())));
        line(out, "  tcpdump -v marks it incorrect; plain tcpdump does not");
    }

    private static void act6WhatItReveals(StringBuilder out) throws BadPacketException {
        line(out, "SIX. What a capture reveals.");
        line(out, "  WHAT THE CUSTOMER SENT (the order, without TLS):");
        line(out, "    " + Traffic.ORDER);
        byte[] order = PcapFile.read(PcapFile.write(Traffic.purchase())).get(3);
        String text = new String(order, StandardCharsets.ISO_8859_1);
        int start = text.indexOf("card=");
        line(out, "  WHAT THE CAPTURE HOLDS (packet 4, bytes " + start + " to " + (order.length - 1) + "):");
        line(out, "    " + text.substring(start));
        line(out, "    the capture contains the card number: " + Capture.containsText(PcapFile.write(Traffic.purchase()), "4111111111111111"));
        line(out, "  A CAPTURE FILE IS AS SENSITIVE AS THE TRAFFIC");
        line(out, "  protect it like a password; capture only what you need");
        line(out, "  with TLS, a thief would see only scrambled bytes here");
    }

    private static String hex(List<Integer> words) {
        StringBuilder text = new StringBuilder();
        for (int word : words) {
            text.append(text.length() == 0 ? "" : " ").append(String.format("%04x", word));
        }
        return text.toString();
    }
}
