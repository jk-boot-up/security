package com.jk.explore.security.integrity;

import java.util.Arrays;
import java.util.HexFormat;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. */
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
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String shortHex(String hex) {
        return hex.substring(0, 12);
    }

    private static void act1Crc(StringBuilder out) {
        line(out, "ONE. A checksum for accidents.");
        byte[] original = Hex.bytes(INVOICE.text());
        String crc = Crc32Checksum.of(original);
        line(out, "  invoice " + INVOICE.text() + " has CRC-32 " + crc);
        byte[] damaged = original.clone();
        damaged[damaged.length - 1] ^= 0x01;
        line(out, "  one bit flips on the disk: the CRC is now " + Crc32Checksum.of(damaged) + "  <- ACCIDENT DETECTED");
        byte[] forged = Hex.bytes(INVOICE.withAmount(12).text());
        line(out, "  an attacker changes the amount to 12 and works out a new CRC, " + Crc32Checksum.of(forged)
                + "  <- THE CHECK PASSES");
        line(out, "  a CRC has no secret, so it only guards against accidents");
    }

    private static void act2Hash(StringBuilder out) {
        line(out, "TWO. A cryptographic hash: SHA-256.");
        line(out, "  SHA-256 of the three letters abc begins " + shortHex(Sha256.hex("abc")));
        String original = Sha256.hex(INVOICE.text());
        String changed = Sha256.hex(INVOICE.withAmount(1201).text());
        line(out, "  invoice at 1200 dollars begins " + shortHex(original));
        line(out, "  invoice at 1201 dollars begins " + shortHex(changed));
        line(out, "  one digit changed, and " + Hex.differingCharacters(original, changed) + " of the 64 hex digits differ");
        int bits = Hex.differingBits(Sha256.digest(Hex.bytes(INVOICE.text())), Sha256.digest(Hex.bytes(INVOICE.withAmount(1201).text())));
        line(out, "  that is " + bits + " of 256 bits: about half, whatever the input");
        line(out, "  the hash is always " + original.length() + " hex digits, for one line or for a whole book");
    }

    private static void act3HashBesideTheFile(StringBuilder out) {
        line(out, "THREE. A hash kept beside the data.");
        InvoiceStore store = new InvoiceStore();
        store.save(INVOICE);
        store.tamper(INVOICE.withAmount(12), false);
        line(out, "  an attacker changes the invoice to 12 dollars, and leaves the hash: intact? " + store.looksIntact("INV-1001"));
        InvoiceStore both = new InvoiceStore();
        both.save(INVOICE);
        both.tamper(INVOICE.withAmount(12), true);
        line(out, "  an attacker changes the invoice and the hash beside it: intact? " + both.looksIntact("INV-1001")
                + "  <- THE ATTACKER WROTE BOTH");
        byte[] installer = Hex.bytes("shop-app-installer-v1");
        byte[] mirrorCopy = Hex.bytes("shop-app-installer-v1-with-malware");
        String published = Sha256.hex(installer);
        line(out, "  the shop publishes the installer's hash on its own site, over HTTPS: " + shortHex(published));
        line(out, "  a download from a mirror hashes to " + shortHex(Sha256.hex(mirrorCopy)) + ": "
                + (Sha256.hex(mirrorCopy).equals(published) ? "accepted" : "refused")
                + "  <- THE HASH CAME BY ANOTHER ROAD");
    }

    private static void act4Hmac(StringBuilder out) {
        line(out, "FOUR. A keyed hash: HMAC-SHA-256.");
        SignedInvoices service = new SignedInvoices(KEY, SignedInvoices.Encoding.LENGTH_PREFIXED, false);
        String tag = service.tag(INVOICE);
        line(out, "  the order service tags invoice INV-1001: " + shortHex(tag));
        line(out, "  the invoice with its tag: " + service.accept(INVOICE, tag));
        Invoice forged = INVOICE.withAmount(12);
        line(out, "  an attacker changes the amount, keeps the tag: " + service.accept(forged, tag));
        String plain = Sha256.hex(forged.text());
        line(out, "  the attacker tags it with a plain SHA-256 instead: " + service.accept(forged, plain));
        String wrongKey = Hmac.hex(Hex.bytes("a guess at the key"), service.encode(forged));
        line(out, "  the attacker tags it with HMAC and a guessed key: " + service.accept(forged, wrongKey));
        line(out, "  without the key, no tag the service accepts can be made");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: boundaries, repeats, and a shared key.");
        SignedInvoices joined = new SignedInvoices(KEY, SignedInvoices.Encoding.JOINED, false);
        Invoice first = new Invoice("INV-7", "ab", "c", 5);
        Invoice moved = new Invoice("INV-7", "a", "bc", 5);
        line(out, "  fields joined with nothing between: (ab, c) and (a, bc) get tags "
                + shortHex(joined.tag(first)) + " and " + shortHex(joined.tag(moved)));
        line(out, "  the attacker moves a field boundary, keeping the tag: " + joined.accept(moved, joined.tag(first))
                + "  <- THE BOUNDARY MOVED");
        SignedInvoices prefixed = new SignedInvoices(KEY, SignedInvoices.Encoding.LENGTH_PREFIXED, false);
        line(out, "  with each field's length in front: " + prefixed.accept(moved, prefixed.tag(first)));

        SignedInvoices open = new SignedInvoices(KEY, SignedInvoices.Encoding.LENGTH_PREFIXED, false);
        String tag = open.tag(INVOICE);
        line(out, "  a valid invoice is sent twice: " + open.accept(INVOICE, tag) + ", " + open.accept(INVOICE, tag)
                + "  <- A TAG DOES NOT SAY WHEN");
        SignedInvoices remembering = new SignedInvoices(KEY, SignedInvoices.Encoding.LENGTH_PREFIXED, true);
        line(out, "  when the service remembers invoice numbers: " + remembering.accept(INVOICE, tag) + ", "
                + remembering.accept(INVOICE, tag));

        SignedInvoices warehouse = new SignedInvoices(KEY, SignedInvoices.Encoding.LENGTH_PREFIXED, false);
        Invoice made = new Invoice("INV-9000", "mallory", "Laptop", 1);
        line(out, "  the warehouse holds the same key to check tags, and can make one: "
                + warehouse.accept(made, warehouse.tag(made)) + "  <- KEY HOLDERS CANNOT BE TOLD APART");
        line(out, "  to prove who made a tag, a signature with a private key is needed: the next projects");
    }
}
