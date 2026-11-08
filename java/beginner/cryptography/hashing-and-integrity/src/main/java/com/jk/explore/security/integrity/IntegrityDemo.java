package com.jk.explore.security.integrity;

import java.util.Arrays;

/** Runs the online store story in six numbered acts and prints exact, repeatable results. */
public class IntegrityDemo {

    static final Invoice INVOICE = new Invoice("INV-1001", "ana", "Laptop", 1200);
    static final byte[] KEY = "demo-only-invoice-key-not-for-real-use".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1Crc(out);
        act2Hash(out);
        act3HashBesideTheFile(out);
        act4Hmac(out);
        act5Steps(out);
        act6Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String shortHex(String hex) {
        return hex.substring(0, 12);
    }

    /** Hex in groups of 8 digits (4 bytes), 4 groups per line, the next lines indented under the first. */
    static String grouped(byte[] bytes, String indent) {
        String hex = Hex.of(bytes);
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 8) {
            if (i > 0) {
                s.append(i % 32 == 0 ? "\n" + indent : " ");
            }
            s.append(hex, i, Math.min(i + 8, hex.length()));
        }
        return s.toString();
    }

    private static final String UNDER = " ".repeat(15);   // lines up under a value after "    label      "

    private static void act1Crc(StringBuilder out) {
        line(out, "ONE. A checksum for accidents.");
        byte[] original = Hex.bytes(INVOICE.text());
        line(out, "  BEFORE THE CHANGE (the invoice as stored):");
        line(out, "    invoice    " + INVOICE.text());
        line(out, "    CRC-32     " + Crc32Checksum.of(original));
        byte[] damaged = original.clone();
        damaged[damaged.length - 1] ^= 0x01;                  // one bit flips: '0' becomes '1'
        line(out, "  AFTER AN ACCIDENT (one bit flips on the disk):");
        line(out, "    invoice    " + new String(damaged, java.nio.charset.StandardCharsets.UTF_8));
        line(out, "    CRC-32     " + Crc32Checksum.of(damaged) + "  <- DOES NOT MATCH: DETECTED");
        Invoice forged = INVOICE.withAmount(12);
        line(out, "  AFTER AN ATTACK (amount changed, a new CRC stored):");
        line(out, "    invoice    " + forged.text());
        line(out, "    CRC-32     " + Crc32Checksum.of(Hex.bytes(forged.text())) + "  <- MATCHES: THE CHECK PASSES");
        line(out, "  a CRC has no secret, so it only guards against accidents");
    }

    private static void act2Hash(StringBuilder out) {
        line(out, "TWO. A cryptographic hash: SHA-256.");
        line(out, "  the standard's test: abc gives " + shortHex(Sha256.hex("abc")) + "...  <- AS PUBLISHED");
        Invoice changed = INVOICE.withAmount(1201);
        line(out, "  BEFORE HASHING (two invoices, one digit apart):");
        line(out, "    invoice 1  " + INVOICE.text());
        line(out, "    invoice 2  " + changed.text());
        byte[] h1 = Sha256.digest(Hex.bytes(INVOICE.text()));
        byte[] h2 = Sha256.digest(Hex.bytes(changed.text()));
        line(out, "  AFTER HASHING (32 bytes each, 64 hex digits):");
        line(out, "    hash 1     " + grouped(h1, UNDER));
        line(out, "    hash 2     " + grouped(h2, UNDER));
        line(out, "  " + Hex.differingCharacters(Hex.of(h1), Hex.of(h2)) + " of the 64 hex digits differ, "
                + Hex.differingBits(h1, h2) + " of the 256 bits: about half");
        line(out, "  any input, a line or a whole book: always " + Hex.of(h1).length() + " hex digits");
    }

    private static void act3HashBesideTheFile(StringBuilder out) {
        line(out, "THREE. A hash kept beside the data.");
        Invoice forged = INVOICE.withAmount(12);
        line(out, "  BEFORE THE ATTACK (the store keeps both):");
        line(out, "    invoice    " + INVOICE.text());
        line(out, "    hash       " + shortHex(Sha256.hex(INVOICE.text())) + "...");
        InvoiceStore store = new InvoiceStore();
        store.save(INVOICE);
        store.tamper(forged, false);
        line(out, "  AFTER ATTACK 1 (the invoice changed, the hash left):");
        line(out, "    invoice    " + store.get("INV-1001").text());
        line(out, "    hash       " + shortHex(store.storedHash("INV-1001")) + "...  intact? "
                + store.looksIntact("INV-1001") + "  <- CAUGHT");
        InvoiceStore both = new InvoiceStore();
        both.save(INVOICE);
        both.tamper(forged, true);
        line(out, "  AFTER ATTACK 2 (the invoice and the hash changed):");
        line(out, "    invoice    " + both.get("INV-1001").text());
        line(out, "    hash       " + shortHex(both.storedHash("INV-1001")) + "...  intact? "
                + both.looksIntact("INV-1001") + "  <- NOT CAUGHT");
        byte[] installer = Hex.bytes("shop-app-installer-v1");
        byte[] mirrorCopy = Hex.bytes("shop-app-installer-v1-with-malware");
        String published = Sha256.hex(installer);
        String downloaded = Sha256.hex(mirrorCopy);
        line(out, "  A HASH BY ANOTHER ROAD (the shop's installer):");
        line(out, "    on the shop's own site, over HTTPS  " + shortHex(published) + "...");
        line(out, "    the file from a mirror hashes to    " + shortHex(downloaded) + "...");
        line(out, "    " + (downloaded.equals(published) ? "accepted" : "refused")
                + "  <- THE MIRROR CANNOT CHANGE THE SHOP'S SITE");
    }

    private static void act4Hmac(StringBuilder out) {
        line(out, "FOUR. A keyed hash: HMAC-SHA-256.");
        SignedInvoices service = new SignedInvoices(KEY, SignedInvoices.Encoding.LENGTH_PREFIXED, false);
        line(out, "  BEFORE TAGGING (the inputs):");
        line(out, "    key        " + new String(KEY, java.nio.charset.StandardCharsets.UTF_8));
        line(out, "    invoice    " + INVOICE.text());
        byte[] message = service.encode(INVOICE);
        line(out, "    as bytes   " + grouped(message, UNDER));
        String tag = service.tag(INVOICE);
        line(out, "  AFTER TAGGING (the tag, 32 bytes):");
        line(out, "    tag        " + grouped(java.util.HexFormat.of().parseHex(tag), UNDER));
        line(out, "  THE CHECKS:");
        Invoice forged = INVOICE.withAmount(12);
        line(out, "    the invoice with its tag            " + service.accept(INVOICE, tag));
        line(out, "    amount changed to 12, old tag       " + service.accept(forged, tag));
        line(out, "    a plain SHA-256 as the tag          " + service.accept(forged, Sha256.hex(forged.text())));
        String wrongKey = Hmac.hex(Hex.bytes("a guess at the key"), service.encode(forged));
        line(out, "    HMAC made with a guessed key        " + service.accept(forged, wrongKey));
        line(out, "  without the key, no tag the service accepts can be made");
    }

    private static void act5Steps(StringBuilder out) {
        line(out, "FIVE. How the tag is made, step by step.");
        SignedInvoices service = new SignedInvoices(KEY, SignedInvoices.Encoding.LENGTH_PREFIXED, false);
        byte[] message = service.encode(INVOICE);
        HmacSteps.Steps s = HmacSteps.of(KEY, message);
        String more = " ...";
        line(out, "  BEFORE: THE KEY, PADDED WITH ZEROS TO 64 BYTES:");
        line(out, "    key        " + grouped(Arrays.copyOf(s.paddedKey(), 16), UNDER) + more);
        line(out, "  STEP 1. INNER KEY = KEY XOR 36 36 36 ...:");
        line(out, "    inner key  " + grouped(Arrays.copyOf(s.innerKey(), 16), UNDER) + more);
        line(out, "  STEP 2. INNER HASH = SHA-256(inner key + bytes):");
        line(out, "    inner hash " + grouped(s.innerHash(), UNDER));
        line(out, "  STEP 3. OUTER KEY = KEY XOR 5c 5c 5c ...:");
        line(out, "    outer key  " + grouped(Arrays.copyOf(s.outerKey(), 16), UNDER) + more);
        line(out, "  STEP 4. TAG = SHA-256(outer key + inner hash):");
        line(out, "    tag        " + grouped(s.tag(), UNDER));
        line(out, "  AFTER: the same tag Java's Mac gave in act four: "
                + Arrays.equals(s.tag(), Hmac.tag(KEY, message)));
    }

    private static void act6Bill(StringBuilder out) {
        line(out, "SIX. The bill: boundaries, repeats, and a shared key.");
        Invoice first = new Invoice("INV-7", "ab", "c", 5);
        Invoice moved = new Invoice("INV-7", "a", "bc", 5);
        SignedInvoices joined = new SignedInvoices(KEY, SignedInvoices.Encoding.JOINED, false);
        line(out, "  BEFORE THE FIX (fields joined, nothing between):");
        line(out, "    (ab, c)    bytes " + new String(joined.encode(first), java.nio.charset.StandardCharsets.UTF_8)
                + "  tag " + shortHex(joined.tag(first)));
        line(out, "    (a, bc)    bytes " + new String(joined.encode(moved), java.nio.charset.StandardCharsets.UTF_8)
                + "  tag " + shortHex(joined.tag(moved)));
        line(out, "    boundary moved, old tag: " + joined.accept(moved, joined.tag(first)) + "  <- THE SAME BYTES");
        SignedInvoices prefixed = new SignedInvoices(KEY, SignedInvoices.Encoding.LENGTH_PREFIXED, false);
        line(out, "  AFTER THE FIX (each field with its length in front):");
        line(out, "    (ab, c)    tag " + shortHex(prefixed.tag(first)));
        line(out, "    (a, bc)    tag " + shortHex(prefixed.tag(moved)));
        line(out, "    boundary moved, old tag: " + prefixed.accept(moved, prefixed.tag(first)));

        SignedInvoices forgets = new SignedInvoices(KEY, SignedInvoices.Encoding.LENGTH_PREFIXED, false);
        String tag = forgets.tag(INVOICE);
        line(out, "  BEFORE THE FIX (the service forgets):");
        line(out, "    one invoice sent twice: " + forgets.accept(INVOICE, tag) + ", " + forgets.accept(INVOICE, tag)
                + "  <- A TAG HAS NO DATE");
        SignedInvoices remembering = new SignedInvoices(KEY, SignedInvoices.Encoding.LENGTH_PREFIXED, true);
        line(out, "  AFTER THE FIX (it remembers invoice numbers):");
        line(out, "    one invoice sent twice: " + remembering.accept(INVOICE, tag) + ", "
                + remembering.accept(INVOICE, tag));
        SignedInvoices warehouse = new SignedInvoices(KEY, SignedInvoices.Encoding.LENGTH_PREFIXED, false);
        Invoice made = new Invoice("INV-9000", "mallory", "Laptop", 1);
        line(out, "  NO FIX WITH HMAC (the warehouse holds the key too):");
        line(out, "    it tags INV-9000, a laptop for 1 dollar: " + warehouse.accept(made, warehouse.tag(made))
                + "  <- WHO?");
        line(out, "  a signature proves who made a tag: the next projects");
    }
}
