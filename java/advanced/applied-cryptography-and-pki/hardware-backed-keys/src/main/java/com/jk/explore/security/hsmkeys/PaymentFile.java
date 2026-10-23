package com.jk.explore.security.hsmkeys;

import java.nio.charset.StandardCharsets;

/** A made-up bank payment file: the thing the key signs. */
final class PaymentFile {

    private PaymentFile() {
    }

    static byte[] content(String id, int payments, long totalCents) {
        StringBuilder sb = new StringBuilder("FILE " + id + "\n");
        for (int i = 1; i <= payments; i++) {
            sb.append("PAY ").append(i).append(" ACC-").append(1000 + i).append(" -> ACC-").append(2000 + i).append("\n");
        }
        sb.append("TOTAL ").append(totalCents).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
