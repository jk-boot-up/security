package com.jk.explore.security.tcpipdump;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Reads a capture: prints each packet in the style of `tcpdump -nn -t -S`, counts damaged packets, and finds sources that started a connection and never finished it. */
final class Capture {

    private Capture() {
    }

    /** One line per packet, in the format of `tcpdump -nn -t -S -r file`. */
    static List<String> lines(byte[] file) throws BadPacketException {
        List<String> lines = new ArrayList<>();
        for (byte[] raw : PcapFile.read(file)) {
            Ipv4Packet packet = Ipv4Packet.parseUnchecked(raw);
            TcpSegment segment = TcpSegment.parse(packet.payload());
            lines.add(describe(packet, segment));
        }
        return lines;
    }

    static String describe(Ipv4Packet packet, TcpSegment segment) {
        StringBuilder text = new StringBuilder("IP ");
        text.append(Address.text(packet.source())).append('.').append(segment.sourcePort());
        text.append(" > ").append(Address.text(packet.destination())).append('.').append(segment.destinationPort());
        text.append(": Flags [").append(flags(segment)).append(']');
        int length = segment.payload().length;
        // like tcpdump, a bare ACK carries no new sequence number worth showing
        if (length > 0 || segment.has(TcpSegment.SYN) || segment.has(TcpSegment.FIN) || segment.has(TcpSegment.RST)) {
            text.append(", seq ").append(segment.sequence());
            if (length > 0) {
                text.append(':').append((segment.sequence() + length) & 0xffffffffL);
            }
        }
        if (segment.has(TcpSegment.ACK)) {
            text.append(", ack ").append(segment.acknowledgement());
        }
        return text.append(", win ").append(segment.window()).append(", length ").append(length).toString();
    }

    private static String flags(TcpSegment segment) {
        StringBuilder text = new StringBuilder();
        if (segment.has(TcpSegment.FIN)) {
            text.append('F');
        }
        if (segment.has(TcpSegment.SYN)) {
            text.append('S');
        }
        if (segment.has(TcpSegment.RST)) {
            text.append('R');
        }
        if (segment.has(TcpSegment.PSH)) {
            text.append('P');
        }
        if (segment.has(TcpSegment.ACK)) {
            text.append('.');
        }
        return text.length() == 0 ? "none" : text.toString();
    }

    /** How many packets have a header or TCP checksum that does not match. */
    static int damagedCount(byte[] file) throws BadPacketException {
        int damaged = 0;
        for (byte[] raw : PcapFile.read(file)) {
            try {
                Ipv4Packet packet = Ipv4Packet.decode(raw);
                if (!TcpSegment.checksumOk(packet.source(), packet.destination(), packet.payload())) {
                    damaged++;
                }
            } catch (BadPacketException e) {
                damaged++;
            }
        }
        return damaged;
    }

    /** Sources that sent a SYN to a server and never sent the final ACK of the handshake. */
    static Set<String> unfinishedHandshakes(byte[] file) throws BadPacketException {
        Map<String, Boolean> finished = new LinkedHashMap<>();
        for (byte[] raw : PcapFile.read(file)) {
            Ipv4Packet packet = Ipv4Packet.parseUnchecked(raw);
            TcpSegment segment = TcpSegment.parse(packet.payload());
            String key = Address.text(packet.source()) + ":" + segment.sourcePort();
            if (segment.has(TcpSegment.SYN) && !segment.has(TcpSegment.ACK)) {
                finished.putIfAbsent(key, false);
            } else if (segment.has(TcpSegment.ACK) && !segment.has(TcpSegment.SYN) && finished.containsKey(key)) {
                finished.put(key, true);
            }
        }
        Set<String> unfinished = new TreeSet<>();
        finished.forEach((key, done) -> {
            if (!done) {
                unfinished.add(key.substring(0, key.indexOf(':')));
            }
        });
        return unfinished;
    }

    static boolean containsText(byte[] file, String text) throws BadPacketException {
        for (byte[] raw : PcapFile.read(file)) {
            if (new String(raw, java.nio.charset.StandardCharsets.ISO_8859_1).contains(text)) {
                return true;
            }
        }
        return false;
    }
}
