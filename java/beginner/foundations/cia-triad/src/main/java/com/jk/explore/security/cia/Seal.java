package com.jk.explore.security.cia;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Makes and checks the seal on an order: a short code worked out from the order and a secret
 * key. Change one character of the order and the code no longer matches. The key here is a
 * fixed demo value; a real system keeps it out of the code (a later project explains how).
 */
public final class Seal {

    private final SecretKeySpec key;

    public Seal(String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String of(OrderRecord order) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] code = mac.doFinal(order.sealedText().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(code);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is part of every Java runtime", e);
        }
    }

    public boolean matches(OrderRecord order, String seal) {
        return of(order).equals(seal);
    }
}
