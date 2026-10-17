package com.jk.explore.security.integrity;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * The order service's way of tagging invoices with HMAC, in two versions of how an invoice becomes bytes: fields simply
 * joined together, which is ambiguous, and fields each written with their length in front, which is not.
 */
public class SignedInvoices {

    /** How an invoice's fields are turned into the bytes that are tagged. */
    public enum Encoding {
        /** The fields run together with nothing between them. */
        JOINED,
        /** Each field is written as its length, then its bytes. */
        LENGTH_PREFIXED
    }

    private final byte[] key;
    private final Encoding encoding;
    private final boolean rememberNumbers;
    private final Set<String> seen = new HashSet<>();

    public SignedInvoices(byte[] key, Encoding encoding, boolean rememberNumbers) {
        this.key = key.clone();
        this.encoding = encoding;
        this.rememberNumbers = rememberNumbers;
    }

    public String tag(Invoice invoice) {
        return Hmac.hex(key, encode(invoice));
    }

    /** Checks an invoice and its tag, and with {@code rememberNumbers} refuses an invoice number that was accepted before. */
    public Verdict accept(Invoice invoice, String tag) {
        if (!Hmac.verify(key, encode(invoice), java.util.HexFormat.of().parseHex(tag))) {
            return Verdict.BAD_TAG;
        }
        if (rememberNumbers && !seen.add(invoice.number())) {
            return Verdict.REPLAYED;
        }
        return Verdict.OK;
    }

    byte[] encode(Invoice invoice) {
        String[] fields = {invoice.number(), invoice.customer(), invoice.item(), Integer.toString(invoice.amountDollars())};
        if (encoding == Encoding.JOINED) {
            return String.join("", fields).getBytes(StandardCharsets.UTF_8);
        }
        int size = 0;
        for (String f : fields) {
            size += 4 + f.getBytes(StandardCharsets.UTF_8).length;
        }
        ByteBuffer out = ByteBuffer.allocate(size);
        for (String f : fields) {
            byte[] b = f.getBytes(StandardCharsets.UTF_8);
            out.putInt(b.length).put(b);
        }
        return out.array();
    }

    /** The answer to a checked invoice. */
    public enum Verdict {
        OK,
        BAD_TAG,
        REPLAYED
    }
}
