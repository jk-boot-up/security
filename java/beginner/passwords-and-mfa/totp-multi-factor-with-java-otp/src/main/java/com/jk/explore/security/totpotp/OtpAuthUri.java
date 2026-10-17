package com.jk.explore.security.totpotp;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base32;

/**
 * The text that a QR code carries when you turn on two-step sign-in: an {@code otpauth://} link holding the issuer, the
 * account, the secret as Base32 and the settings. The authenticator app reads it once and keeps the secret.
 */
public final class OtpAuthUri {

    private OtpAuthUri() {
    }

    public static String totp(String issuer, String account, byte[] secret, String algorithm, int digits, int periodSeconds) {
        String label = encode(issuer) + ":" + encode(account);
        String base32 = new Base32().encodeToString(secret).replace("=", "");
        return "otpauth://totp/" + label + "?secret=" + base32 + "&issuer=" + encode(issuer) + "&algorithm=" + algorithm
                + "&digits=" + digits + "&period=" + periodSeconds;
    }

    private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
