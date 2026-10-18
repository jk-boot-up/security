package com.jk.explore.security.passkeys;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The client data that the browser, not the web page, writes for every WebAuthn call: the type of call, the server's challenge and
 * the origin of the page that asked. The authenticator signs a hash of exactly these bytes, so the origin cannot be changed afterwards.
 */
record ClientData(String type, String challenge, String origin) {

    static final String CREATE = "webauthn.create";
    static final String GET = "webauthn.get";

    /** The JSON in the member order WebAuthn Level 3 specifies for its limited serialization. */
    byte[] json() {
        String text = "{\"type\":" + quote(type) + ",\"challenge\":" + quote(challenge) + ",\"origin\":" + quote(origin)
                + ",\"crossOrigin\":false}";
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c < 0x20) {
                out.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.append('"').toString();
    }

    /** Reads client data strictly: one flat JSON object of string and boolean members, no duplicates, the three members required. */
    static ClientData parse(byte[] bytes) {
        Map<String, String> members = new LinkedHashMap<>();
        String text = new String(bytes, StandardCharsets.UTF_8);
        int[] at = {0};
        skip(text, at);
        expect(text, at, '{');
        skip(text, at);
        if (peek(text, at) != '}') {
            while (true) {
                skip(text, at);
                String name = string(text, at);
                skip(text, at);
                expect(text, at, ':');
                skip(text, at);
                String value;
                char c = peek(text, at);
                if (c == '"') {
                    value = string(text, at);
                } else if (text.startsWith("true", at[0]) || text.startsWith("false", at[0])) {
                    value = text.startsWith("true", at[0]) ? "true" : "false";
                    at[0] += value.length();
                } else {
                    throw new IllegalArgumentException("client data member " + name + " is not a string or a boolean");
                }
                if (members.put(name, value) != null) {
                    throw new IllegalArgumentException("client data has the member " + name + " twice");
                }
                skip(text, at);
                char next = peek(text, at);
                at[0]++;
                if (next == '}') {
                    break;
                }
                if (next != ',') {
                    throw new IllegalArgumentException("client data is not a JSON object");
                }
            }
        } else {
            at[0]++;
        }
        skip(text, at);
        if (at[0] != text.length()) {
            throw new IllegalArgumentException("text after the client data object");
        }
        for (String required : new String[] {"type", "challenge", "origin"}) {
            if (!members.containsKey(required)) {
                throw new IllegalArgumentException("client data has no " + required);
            }
        }
        return new ClientData(members.get("type"), members.get("challenge"), members.get("origin"));
    }

    private static char peek(String text, int[] at) {
        if (at[0] >= text.length()) {
            throw new IllegalArgumentException("client data ends early");
        }
        return text.charAt(at[0]);
    }

    private static void expect(String text, int[] at, char wanted) {
        if (peek(text, at) != wanted) {
            throw new IllegalArgumentException("client data is not a JSON object");
        }
        at[0]++;
    }

    private static void skip(String text, int[] at) {
        while (at[0] < text.length() && " \t\r\n".indexOf(text.charAt(at[0])) >= 0) {
            at[0]++;
        }
    }

    private static String string(String text, int[] at) {
        expect(text, at, '"');
        StringBuilder out = new StringBuilder();
        while (true) {
            char c = peek(text, at);
            at[0]++;
            if (c == '"') {
                return out.toString();
            }
            if (c == '\\') {
                char e = peek(text, at);
                at[0]++;
                switch (e) {
                    case '"', '\\', '/' -> out.append(e);
                    case 'u' -> {
                        out.append((char) Integer.parseInt(text.substring(at[0], at[0] + 4), 16));
                        at[0] += 4;
                    }
                    default -> throw new IllegalArgumentException("unsupported escape in client data");
                }
            } else if (c < 0x20) {
                throw new IllegalArgumentException("control character in client data");
            } else {
                out.append(c);
            }
        }
    }
}
