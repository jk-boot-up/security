package com.jk.explore.security.dnscoredns;

import java.util.Arrays;

/**
 * The question of act one and its answer, field by field, as bytes on the wire. The question is exactly what the client
 * sends; the answer is the same message with one record added, laid out as DnsMessage writes it. Every note (the time to
 * live, the address) is read back from the bytes, so it shows how those values are made. Needs no Docker.
 */
final class WireSteps {

    static final int ID = 0x1001;

    private WireSteps() {
    }

    static String text() throws BadMessageException {
        DnsMessage query = DnsMessage.query(ID, CoreDnsDemo.WWW);
        byte[] q = query.encode();
        byte[] a = query.answeredWith(ShopZone.GOOD_TTL, Address.parse(ShopZone.GOOD_ADDRESS)).encode();
        StringBuilder out = new StringBuilder();
        out.append("  THE QUESTION (what the client sends over UDP, ").append(q.length).append(" bytes):\n");
        header(out, q, "picked by the client", "a question, recursion desired", "1 question, 0 answers");
        row(out, "name", labels(q, 12), "");
        row(out, "type", hex(q, q.length - 4, 2), "A: an IPv4 address");
        row(out, "class", hex(q, q.length - 2, 2), "IN: internet");
        out.append("  THE ANSWER (as DnsMessage writes it, ").append(a.length).append(" bytes):\n");
        header(out, a, "the question's id, repeated", "a response, no error", "1 question, 1 answer");
        int at = q.length;
        row(out, "question", "the same " + (at - 12) + " bytes", "");
        row(out, "name", hex(a, at, 2), "points back to byte " + (((a[at] & 0x3f) << 8) | (a[at + 1] & 0xff)));
        row(out, "type+class", hex(a, at + 2, 4), "A, IN");
        row(out, "ttl", hex(a, at + 6, 4), number(a, at + 6, 4) + " seconds");
        row(out, "length", hex(a, at + 10, 2), number(a, at + 10, 2) + " bytes follow");
        row(out, "address", hex(a, at + 12, 4), Address.text((int) number(a, at + 12, 4)));
        return out.toString();
    }

    private static void header(StringBuilder out, byte[] m, String id, String flags, String counts) {
        row(out, "id", hex(m, 0, 2), id);
        row(out, "flags", hex(m, 2, 2), flags);
        row(out, "counts", hex(m, 4, 4), counts);
        row(out, "", hex(m, 8, 4), "0 other records");
    }

    private static void row(StringBuilder out, String label, String bytes, String note) {
        String line = "    " + pad(label, 11) + (note.isEmpty() ? bytes : pad(bytes, 13) + note);
        out.append(line.stripTrailing()).append('\n');
    }

    private static String pad(String text, int width) {
        return text.length() >= width ? text + " " : text + " ".repeat(width - text.length());
    }

    /** "00 00 01 2c" for four bytes from a position. */
    static String hex(byte[] m, int from, int count) {
        StringBuilder s = new StringBuilder();
        for (int i = from; i < from + count; i++) {
            s.append(i > from ? " " : "").append("%02x".formatted(m[i] & 0xff));
        }
        return s.toString();
    }

    /** Big-endian: the first byte counts most, so 00 00 01 2c is 1 * 256 + 0x2c = 300. */
    static long number(byte[] m, int from, int count) {
        long value = 0;
        for (int i = from; i < from + count; i++) {
            value = (value << 8) | (m[i] & 0xff);
        }
        return value;
    }

    /** A name as written on the wire: each label after its length byte, "03 www 04 shop 07 example 00". */
    static String labels(byte[] m, int from) {
        StringBuilder s = new StringBuilder();
        int at = from;
        while (m[at] != 0) {
            int length = m[at];
            s.append("%02x ".formatted(length)).append(new String(Arrays.copyOfRange(m, at + 1, at + 1 + length),
                    java.nio.charset.StandardCharsets.US_ASCII)).append(' ');
            at += 1 + length;
        }
        return s.append("00").toString();
    }
}
