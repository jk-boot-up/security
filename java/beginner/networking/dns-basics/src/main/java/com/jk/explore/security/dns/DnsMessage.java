package com.jk.explore.security.dns;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A DNS message as in RFC 1035: a 12-byte header, questions, and answers. Only the A record (an IPv4 address, class IN) is supported.
 * Names are written as length-prefixed labels, and a name that repeats a suffix seen earlier is shortened to a two-byte pointer.
 */
record DnsMessage(int id, boolean response, int rcode, List<Question> questions, List<Answer> answers) {

    static final int TYPE_A = 1;
    static final int CLASS_IN = 1;
    private static final int MAX_JUMPS = 16;

    record Question(String name, int type) {
    }

    record Answer(String name, long ttl, int address) {
    }

    static DnsMessage query(int id, String name) {
        return new DnsMessage(id, false, 0, List.of(new Question(name, TYPE_A)), List.of());
    }

    DnsMessage answeredWith(long ttl, int address) {
        return new DnsMessage(id, true, 0, questions, List.of(new Answer(questions.get(0).name(), ttl, address)));
    }

    byte[] encode() throws BadMessageException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Map<String, Integer> offsets = new HashMap<>();
        put16(out, id);
        put16(out, (response ? 0x8180 : 0x0100) | rcode);
        put16(out, questions.size());
        put16(out, answers.size());
        put16(out, 0);
        put16(out, 0);
        for (Question question : questions) {
            name(out, question.name(), offsets);
            put16(out, question.type());
            put16(out, CLASS_IN);
        }
        for (Answer answer : answers) {
            name(out, answer.name(), offsets);
            put16(out, TYPE_A);
            put16(out, CLASS_IN);
            put16(out, (int) (answer.ttl() >> 16));
            put16(out, (int) (answer.ttl() & 0xffff));
            put16(out, 4);
            put16(out, answer.address() >>> 16);
            put16(out, answer.address() & 0xffff);
        }
        return out.toByteArray();
    }

    private static void put16(ByteArrayOutputStream out, int value) {
        out.write((value >> 8) & 0xff);
        out.write(value & 0xff);
    }

    private static void name(ByteArrayOutputStream out, String name, Map<String, Integer> offsets) throws BadMessageException {
        String[] labels = name.isEmpty() ? new String[0] : name.split("\\.", -1);
        if (name.length() > 253) {
            throw new BadMessageException("name longer than 253 characters");
        }
        for (int i = 0; i < labels.length; i++) {
            String suffix = String.join(".", java.util.Arrays.copyOfRange(labels, i, labels.length));
            Integer seen = offsets.get(suffix);
            if (seen != null) {
                put16(out, 0xC000 | seen);
                return;
            }
            byte[] label = labels[i].getBytes(StandardCharsets.US_ASCII);
            if (label.length == 0 || label.length > 63) {
                throw new BadMessageException("a label must be 1 to 63 bytes");
            }
            if (out.size() < 0x3fff) {
                offsets.put(suffix, out.size());
            }
            out.write(label.length);
            out.writeBytes(label);
        }
        out.write(0);
    }

    static DnsMessage decode(byte[] bytes) throws BadMessageException {
        if (bytes.length < 12) {
            throw new BadMessageException("message shorter than a DNS header");
        }
        int id = u16(bytes, 0);
        int flags = u16(bytes, 2);
        int questionCount = u16(bytes, 4);
        int answerCount = u16(bytes, 6);
        int[] position = {12};
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < questionCount; i++) {
            String name = readName(bytes, position);
            int type = u16(bytes, position[0]);
            position[0] += 4;
            questions.add(new Question(name, type));
        }
        List<Answer> answers = new ArrayList<>();
        for (int i = 0; i < answerCount; i++) {
            String name = readName(bytes, position);
            int p = position[0];
            if (p + 10 > bytes.length) {
                throw new BadMessageException("answer cut short");
            }
            int type = u16(bytes, p);
            long ttl = ((long) u16(bytes, p + 4) << 16) | u16(bytes, p + 6);
            int length = u16(bytes, p + 8);
            if (p + 10 + length > bytes.length) {
                throw new BadMessageException("answer data cut short");
            }
            position[0] = p + 10 + length;
            if (type == TYPE_A && length == 4) {
                int address = (u16(bytes, p + 10) << 16) | u16(bytes, p + 12);
                answers.add(new Answer(name, ttl, address));
            }
        }
        return new DnsMessage(id, (flags & 0x8000) != 0, flags & 0x000f, questions, answers);
    }

    private static int u16(byte[] bytes, int at) throws BadMessageException {
        if (at < 0 || at + 2 > bytes.length) {
            throw new BadMessageException("message cut short");
        }
        return ((bytes[at] & 0xff) << 8) | (bytes[at + 1] & 0xff);
    }

    /** Reads a name at position[0], following pointers; position[0] ends after the name as written at that place. */
    private static String readName(byte[] bytes, int[] position) throws BadMessageException {
        StringBuilder name = new StringBuilder();
        int at = position[0];
        int after = -1;
        int jumps = 0;
        while (true) {
            if (at >= bytes.length) {
                throw new BadMessageException("name cut short");
            }
            int length = bytes[at] & 0xff;
            if (length == 0) {
                at++;
                break;
            }
            if ((length & 0xC0) == 0xC0) {
                int target = ((length & 0x3f) << 8) | u8(bytes, at + 1);
                if (after < 0) {
                    after = at + 2;
                }
                if (++jumps > MAX_JUMPS || target >= bytes.length) {
                    throw new BadMessageException("name pointers loop or point outside the message");
                }
                at = target;
                continue;
            }
            if (length > 63 || at + 1 + length > bytes.length) {
                throw new BadMessageException("bad label length");
            }
            if (name.length() > 0) {
                name.append('.');
            }
            name.append(new String(bytes, at + 1, length, StandardCharsets.US_ASCII));
            at += 1 + length;
        }
        position[0] = after >= 0 ? after : at;
        return name.toString();
    }

    private static int u8(byte[] bytes, int at) throws BadMessageException {
        if (at >= bytes.length) {
            throw new BadMessageException("message cut short");
        }
        return bytes[at] & 0xff;
    }
}
