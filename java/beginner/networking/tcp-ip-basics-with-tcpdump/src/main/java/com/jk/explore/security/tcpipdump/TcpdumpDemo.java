package com.jk.explore.security.tcpipdump;

import java.util.List;
import java.util.Set;

/** Runs the online store story in five numbered acts: record traffic to a capture file, read it back, find a flood, find a damaged packet, and see what a capture reveals. */
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
        act4Damaged(out);
        act5WhatItReveals(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Record(StringBuilder out) {
        line(out, "ONE. Recording traffic to a capture file.");
        List<byte[]> purchase = Traffic.purchase();
        byte[] file = PcapFile.write(purchase);
        int bytes = purchase.stream().mapToInt(p -> p.length).sum();
        line(out, "  a customer's purchase is " + purchase.size() + " packets, " + bytes + " bytes");
        line(out, "  in a pcap file it is 24 bytes of file header, then 16 bytes of header and the packet bytes for each packet: " + file.length + " bytes");
        line(out, "  tcpdump, Wireshark and the Python script all read the same file");
    }

    private static void act2ReadBack(StringBuilder out) throws BadPacketException {
        line(out, "TWO. Reading it back, in the style of tcpdump.");
        for (String text : Capture.lines(PcapFile.write(Traffic.purchase()))) {
            line(out, "  " + text);
        }
        line(out, "  S is SYN, a dot is ACK, P is PSH: the handshake is the first three lines, and the order is the fourth");
    }

    private static void act3Flood(StringBuilder out) throws BadPacketException {
        line(out, "THREE. Finding a SYN flood in a capture.");
        byte[] file = PcapFile.write(Traffic.everything());
        Set<String> unfinished = Capture.unfinishedHandshakes(file);
        line(out, "  the capture has " + Traffic.everything().size() + " packets: the purchase, then " + Traffic.FLOOD_SOURCES + " SYNs from addresses that never answer, then one more");
        line(out, "  sources that sent a SYN and never finished the handshake: " + unfinished.size());
        line(out, "  the real customer is among them: " + unfinished.contains(Address.text(Traffic.CUSTOMER)));
        line(out, "  the first of them is " + unfinished.iterator().next() + " and they are all from the same network, which is what a flood often looks like");
    }

    private static void act4Damaged(StringBuilder out) throws BadPacketException {
        line(out, "FOUR. Finding a damaged packet.");
        byte[] file = PcapFile.write(Traffic.everything());
        line(out, "  packets whose checksum does not match: " + Capture.damagedCount(file));
        line(out, "  tcpdump without -v shows it like any other; with -v it marks the checksum incorrect");
        line(out, "  a capture shows what arrived, damaged or forged, which is why it is the first thing to look at");
    }

    private static void act5WhatItReveals(StringBuilder out) throws BadPacketException {
        line(out, "FIVE. What a capture reveals.");
        byte[] file = PcapFile.write(Traffic.purchase());
        line(out, "  the capture contains the card number: " + Capture.containsText(file, "4111111111111111") + "  <- A CAPTURE FILE IS AS SENSITIVE AS THE TRAFFIC");
        line(out, "  so protect capture files like passwords, and capture only what the investigation needs");
        line(out, "  live capture needs root, so analyse recorded files without it, on a copy");
        line(out, "  encryption with TLS is what makes a capture useless to a thief");
    }
}
