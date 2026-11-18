package com.jk.explore.security.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Redoes the S256 code challenge by hand, one step at a time, so the demo can print every step with real values. The Java library hides
 * the base64url step; here it is written out, and a test proves the result is exactly {@link Pkce#challenge(String)}.
 */
final class ChallengeSteps {

    /** The base64url alphabet (RFC 4648 section 5): '+' and '/' of plain base64 are replaced by '-' and '_', which are safe in a URL. */
    static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    private ChallengeSteps() {
    }

    /** Step 1: the verifier's characters as ASCII bytes, one byte per character. */
    static byte[] ascii(String verifier) {
        return verifier.getBytes(StandardCharsets.US_ASCII);
    }

    /** Step 2: SHA-256 of those bytes, always 32 bytes. */
    static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }

    /** Step 3: base64url with no '=' padding. Every 3 bytes (24 bits) become 4 characters of 6 bits; a last 2 bytes become 3. */
    static String base64Url(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < bytes.length; i += 3) {
            int n = Math.min(3, bytes.length - i);
            int bits = 0;
            for (int k = 0; k < 3; k++) {
                bits = (bits << 8) | (k < n ? bytes[i + k] & 0xff : 0);
            }
            for (int k = 0; k <= n; k++) {
                out.append(ALPHABET.charAt((bits >> (18 - 6 * k)) & 63));
            }
        }
        return out.toString();
    }

    /** The whole S256 challenge, by hand. */
    static String challenge(String verifier) {
        return base64Url(sha256(ascii(verifier)));
    }

    /** Bytes as lowercase hex pairs separated by spaces, such as "13 d3 1e". */
    static String hex(byte[] bytes, int from, int to) {
        List<String> pairs = new ArrayList<>();
        for (int i = from; i < to; i++) {
            pairs.add(String.format("%02x", bytes[i] & 0xff));
        }
        return String.join(" ", pairs);
    }

    /** The first 3 bytes worked out: "13 d3 1e = 000100 111101 001100 011110 = 4 61 12 30 = E9Me". */
    static String firstGroup(byte[] bytes) {
        int bits = ((bytes[0] & 0xff) << 16) | ((bytes[1] & 0xff) << 8) | (bytes[2] & 0xff);
        List<String> groups = new ArrayList<>();
        List<String> numbers = new ArrayList<>();
        StringBuilder chars = new StringBuilder();
        for (int k = 0; k < 4; k++) {
            int six = (bits >> (18 - 6 * k)) & 63;
            String binary = Integer.toBinaryString(six);
            groups.add("0".repeat(6 - binary.length()) + binary);
            numbers.add(String.valueOf(six));
            chars.append(ALPHABET.charAt(six));
        }
        return hex(bytes, 0, 3) + " = " + String.join(" ", groups) + " = " + String.join(" ", numbers) + " = " + chars;
    }
}
