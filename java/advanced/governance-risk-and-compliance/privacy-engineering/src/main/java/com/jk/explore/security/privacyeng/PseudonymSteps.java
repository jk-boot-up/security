package com.jk.explore.security.privacyeng;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** A teaching class: how a pseudonym is made, step by step: the input text, the whole HMAC-SHA256, and its first 8 bytes. */
public record PseudonymSteps(String input, String fullMac, String pseudonym) {

    public static PseudonymSteps of(String secret, int customerId) throws GeneralSecurityException {
        String input = "customer:" + customerId;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String full = HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        return new PseudonymSteps(input, full, full.substring(0, 16));    // 8 bytes are 16 hex digits
    }
}
