package com.jk.explore.security.validation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Size limits are validation too: a request body, a list and a text field each have a maximum, checked before the work they would cost. */
public final class Limits {

    public static final int MAX_BODY_BYTES = 16 * 1024;
    public static final int MAX_CART_LINES = 50;

    private Limits() {
    }

    /** Reads at most one byte more than the limit, so a huge body costs the server only the limit to refuse. */
    public static byte[] body(InputStream in) throws IOException, Rejected {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int total = 0;
        while (total <= MAX_BODY_BYTES) {
            int n = in.read(buffer, 0, Math.min(buffer.length, MAX_BODY_BYTES + 1 - total));
            if (n < 0) {
                return out.toByteArray();
            }
            out.write(buffer, 0, n);
            total += n;
        }
        throw new Rejected("body", "larger than " + MAX_BODY_BYTES + " bytes");
    }

    public static <T> List<T> cart(List<T> lines) throws Rejected {
        if (lines.size() > MAX_CART_LINES) {
            throw new Rejected("cart", "more than " + MAX_CART_LINES + " lines");
        }
        return lines;
    }
}
