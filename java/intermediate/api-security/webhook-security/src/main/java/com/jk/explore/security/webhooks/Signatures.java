package com.jk.explore.security.webhooks;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The signature a payment provider puts on each webhook: an HMAC-SHA256, with a secret shared with the store, over the timestamp and the
 * exact raw body, sent in one header as {@code t=<seconds>,v1=<hex>}. Signing the timestamp too means an old message cannot be given a new time.
 */
final class Signatures {

    static final String HEADER = "Shop-Signature";

    /** The parts of a signature header. */
    record Parsed(long timestamp, byte[] mac) {
    }

    private Signatures() {
    }

    static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is part of every Java runtime", e);
        }
    }

    /** What is signed: the timestamp, a dot, and the body exactly as it was sent. */
    static byte[] signedContent(long timestamp, byte[] rawBody) {
        byte[] prefix = (timestamp + ".").getBytes(StandardCharsets.US_ASCII);
        byte[] content = new byte[prefix.length + rawBody.length];
        System.arraycopy(prefix, 0, content, 0, prefix.length);
        System.arraycopy(rawBody, 0, content, prefix.length, rawBody.length);
        return content;
    }

    static String header(byte[] secret, long timestamp, byte[] rawBody) {
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(hmacSha256(secret, signedContent(timestamp, rawBody)));
    }

    /** Reads a header of the form {@code t=<seconds>,v1=<hex>}; anything else is treated as no signature. */
    static Optional<Parsed> parse(String header) {
        if (header == null) {
            return Optional.empty();
        }
        Long timestamp = null;
        byte[] mac = null;
        for (String part : header.split(",", -1)) {
            int equals = part.indexOf('=');
            if (equals < 0) {
                return Optional.empty();
            }
            String name = part.substring(0, equals).strip();
            String value = part.substring(equals + 1).strip();
            try {
                if (name.equals("t") && value.matches("[0-9]{1,12}")) {
                    timestamp = Long.parseLong(value);
                } else if (name.equals("v1") && value.matches("[0-9a-f]{64}")) {
                    mac = HexFormat.of().parseHex(value);
                }
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        return timestamp == null || mac == null ? Optional.empty() : Optional.of(new Parsed(timestamp, mac));
    }

    /** True if the mac matches the content under any of the secrets, compared in constant time so timing reveals nothing. */
    static boolean matchesAny(List<byte[]> secrets, Parsed parsed, byte[] rawBody) {
        boolean any = false;
        byte[] content = signedContent(parsed.timestamp(), rawBody);
        for (byte[] secret : secrets) {
            any |= MessageDigest.isEqual(hmacSha256(secret, content), parsed.mac());
        }
        return any;
    }
}
