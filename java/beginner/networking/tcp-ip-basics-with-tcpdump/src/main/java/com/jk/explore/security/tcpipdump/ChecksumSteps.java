package com.jk.explore.security.tcpipdump;

import java.util.ArrayList;
import java.util.List;

/**
 * The TCP checksum of one packet, worked out in the four steps a person would do by hand: split the pseudo-header and
 * the TCP header (checksum field set to zero) into 16-bit words, add them up, fold the carry back in, and flip every bit.
 * The result is what {@link Checksum#of} gives, and what a correct packet carries in bytes 16 and 17 of its TCP header.
 */
record ChecksumSteps(List<Integer> pseudoWords, List<Integer> tcpWords, long total, int folded, int checksum, int stored) {

    /** The steps for the TCP segment inside an IPv4 packet. */
    static ChecksumSteps forTcp(byte[] ipPacket) throws BadPacketException {
        Ipv4Packet packet = Ipv4Packet.parseUnchecked(ipPacket);
        byte[] segment = packet.payload().clone();
        int stored = ((segment[16] & 0xff) << 8) | (segment[17] & 0xff);
        segment[16] = 0;
        segment[17] = 0;
        byte[] all = TcpSegment.withPseudoHeader(packet.source(), packet.destination(), segment);
        List<Integer> words = words(all);
        long total = 0;
        for (int word : words) {
            total += word;
        }
        long folded = total;
        while ((folded >> 16) != 0) {
            folded = (folded & 0xffff) + (folded >> 16);
        }
        int checksum = ~(int) folded & 0xffff;
        return new ChecksumSteps(words.subList(0, 6), words.subList(6, words.size()), total, (int) folded, checksum, stored);
    }

    /** Step 1: the bytes as 16-bit words, two bytes each. */
    static List<Integer> words(byte[] data) {
        List<Integer> words = new ArrayList<>();
        for (int i = 0; i < data.length; i += 2) {
            int low = i + 1 < data.length ? data[i + 1] & 0xff : 0;
            words.add(((data[i] & 0xff) << 8) | low);
        }
        return words;
    }

    boolean matches() {
        return checksum == stored;
    }
}
