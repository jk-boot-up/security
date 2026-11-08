package com.jk.explore.security.symmetrictink;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.Key;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.KeysetHandle;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HexFormat;

/** Runs the online bank story in six numbered acts, with Google's Tink, and prints exact, repeatable results. */
public class TinkDemo {

    static final int KEY_ID = 1001;
    static final String KEY_TEXT = "demo-statement-key-not-for-real";
    static final byte[] ACCOUNT = "account=ANA-1".getBytes(StandardCharsets.UTF_8);
    static final String MASTER_TEXT = "demo-master-key-standing-for-a-kms";
    static final byte[] LINE = "RENT      -1200\n".getBytes(StandardCharsets.UTF_8);
    static final byte[] NONCE = HexFormat.of().parseHex("000000000000000000000001");
    static final int NEW_KEY_ID = 1002;
    static final String NEW_KEY_TEXT = "demo-statement-key-two-not-real";
    static final int MASTER_ID = 2001;

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        try {
            act1NoNonce(out);
            act2ByHand(out);
            act3Tamper(out);
            act4Rotation(out);
            act5Envelope(out);
            act6Bill(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String text(byte[] bytes) {
        return bytes == null ? "refused" : new String(bytes, StandardCharsets.UTF_8).trim();
    }

    /** Bytes as hex, in groups of four bytes: 00000000 00000000 00000001. */
    static String hex(byte[] bytes) {
        String h = HexFormat.of().formatHex(bytes);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < h.length(); i += 8) {
            out.append(i == 0 ? "" : " ").append(h, i, Math.min(i + 8, h.length()));
        }
        return out.toString();
    }

    private static String start(byte[] message) {
        return HexFormat.of().formatHex(message, 0, 5);
    }

    private static String keys(KeysetHandle keyset) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < keyset.size(); i++) {
            out.append(i == 0 ? "" : ", ").append("key ").append(keyset.getAt(i).getId())
                    .append(keyset.getAt(i).isPrimary() ? " (primary)" : "");
        }
        return out.toString();
    }

    private static void act1NoNonce(StringBuilder out) {
        line(out, "ONE. One call, and no nonce to look after.");
        Bank bank = new Bank(DemoKeysets.aes256Gcm(KEY_ID, KEY_TEXT));
        line(out, "  BEFORE ENCRYPTION (what the bank gives Tink):");
        line(out, "    plain text   " + new String(LINE, StandardCharsets.UTF_8).trim() + "   (" + LINE.length + " bytes)");
        line(out, "    account      " + new String(ACCOUNT, StandardCharsets.UTF_8) + "   (checked, not hidden)");
        line(out, "    keyset       one AES256_GCM key, key id " + KEY_ID);
        byte[] first = bank.seal(ACCOUNT, LINE);
        byte[] second = bank.seal(ACCOUNT, LINE);
        line(out, "  AFTER ENCRYPTION (the same line, sealed twice):");
        line(out, "    message 1    " + first.length + " bytes, starts " + start(first));
        line(out, "    message 2    " + second.length + " bytes, starts " + start(second));
        line(out, "    the two are the same: " + Arrays.equals(first, second) + "  <- A NEW NONCE EACH TIME");
        line(out, "  OPENING message 1 (right key, right account):");
        line(out, "    " + text(bank.openOrNull(ACCOUNT, first)) + "  <- OPENED");
    }

    private static void act2ByHand(StringBuilder out) throws GeneralSecurityException {
        line(out, "TWO. Inside one Tink message, worked out by hand.");
        line(out, "  BEFORE ENCRYPTION (a fixed nonce, only for this demo):");
        line(out, "    key id       " + KEY_ID);
        line(out, "    nonce        " + hex(NONCE));
        line(out, "    account      " + new String(ACCOUNT, StandardCharsets.UTF_8));
        line(out, "    plain text   " + new String(LINE, StandardCharsets.UTF_8).trim());
        byte[] message = byHand();
        TinkLayout.Parts parts = TinkLayout.split(message);
        line(out, "  AFTER ENCRYPTION (" + message.length + " bytes, part by part):");
        line(out, "    version      " + HexFormat.of().formatHex(parts.prefix(), 0, 1));
        line(out, "    key id       " + HexFormat.of().formatHex(parts.prefix(), 1, 5)
                + "   (" + KEY_ID + " = " + KEY_ID / 256 + " x 256 + " + KEY_ID % 256
                + ", " + HexFormat.of().toHexDigits((byte) KEY_ID) + " = " + KEY_ID % 256 + ")");
        line(out, "    nonce        " + hex(parts.nonce()));
        line(out, "    cipher text  " + hex(parts.cipherText()));
        line(out, "    tag          " + hex(parts.tag()));
        line(out, "  1 + 4 + " + TinkLayout.NONCE + " + " + parts.cipherText().length + " + " + TinkLayout.TAG
                + " = " + message.length + " bytes");
        Bank bank = new Bank(DemoKeysets.aes256Gcm(KEY_ID, KEY_TEXT));
        line(out, "  Tink opens the hand-built message: " + text(bank.openOrNull(ACCOUNT, message)) + "  <- SAME LAYOUT");
    }

    /** The message of act two: the demo's key and key id, a fixed nonce, laid out by hand. */
    static byte[] byHand() throws GeneralSecurityException {
        return TinkLayout.seal(DemoKeysets.keyBytes(KEY_TEXT), KEY_ID, NONCE, ACCOUNT, LINE);
    }

    private static void act3Tamper(StringBuilder out) throws GeneralSecurityException {
        line(out, "THREE. Any change is refused.");
        byte[] sealed = byHand();
        byte[] tag = TinkLayout.split(sealed).tag();
        Bank bank = new Bank(DemoKeysets.aes256Gcm(KEY_ID, KEY_TEXT));
        line(out, "  THE SEALED MESSAGE (from act two):");
        line(out, "    tag          " + hex(tag));
        line(out, "  OPENING IT (Tink checks the tag first):");
        line(out, "    right key, right account: " + text(bank.openOrNull(ACCOUNT, sealed)) + "  <- OPENED");
        byte[] flipped = sealed.clone();
        flipped[sealed.length - 1] ^= 0x01;
        line(out, "  THREE CHANGES:");
        line(out, "    last byte " + HexFormat.of().toHexDigits(sealed[sealed.length - 1]) + " -> "
                + HexFormat.of().toHexDigits(flipped[sealed.length - 1]) + ": "
                + text(bank.openOrNull(ACCOUNT, flipped)) + "  <- DETECTED");
        line(out, "    moved to account=MAL-2: "
                + text(bank.openOrNull("account=MAL-2".getBytes(StandardCharsets.UTF_8), sealed)) + "  <- DETECTED");
        Bank other = new Bank(DemoKeysets.aes256Gcm(KEY_ID, "a-completely-different-key-text!"));
        line(out, "    another key, same id " + KEY_ID + ": " + text(other.openOrNull(ACCOUNT, sealed)) + "  <- DETECTED");
    }

    private static void act4Rotation(StringBuilder out) throws GeneralSecurityException {
        line(out, "FOUR. Replacing the key without losing the data.");
        KeysetHandle original = DemoKeysets.aes256Gcm(KEY_ID, KEY_TEXT);
        byte[] old = new Bank(original).seal(ACCOUNT, LINE);
        line(out, "  BEFORE ROTATION:");
        line(out, "    keyset       " + keys(original));
        line(out, "    old message  starts " + start(old));
        KeysetHandle rotated = rotate(original);
        Bank bank = new Bank(rotated);
        byte[] fresh = bank.seal(ACCOUNT, LINE);
        line(out, "  AFTER ROTATION (key " + NEW_KEY_ID + " added and made primary):");
        line(out, "    keyset       " + keys(rotated));
        line(out, "    new message  starts " + start(fresh));
        line(out, "    old message opens: " + text(bank.openOrNull(ACCOUNT, old)));
        line(out, "    new message opens: " + text(bank.openOrNull(ACCOUNT, fresh)));
        KeysetHandle onlyNew = KeysetHandle.newBuilder(rotated).deleteAt(0).build();
        line(out, "  AFTER REMOVING KEY " + KEY_ID + ":");
        line(out, "    keyset       " + keys(onlyNew));
        line(out, "    old message opens: " + text(new Bank(onlyNew).openOrNull(ACCOUNT, old)) + "  <- RE-ENCRYPT FIRST");
        line(out, "    new message opens: " + text(new Bank(onlyNew).openOrNull(ACCOUNT, fresh)));
    }

    /** Rotation: the old keyset plus a new key, which becomes primary. Real code uses withRandomId(). */
    static KeysetHandle rotate(KeysetHandle original) throws GeneralSecurityException {
        Key newKey = DemoKeysets.aes256Gcm(NEW_KEY_ID, NEW_KEY_TEXT).getPrimary().getKey();
        return KeysetHandle.newBuilder(original)
                .addEntry(KeysetHandle.importKey(newKey).withFixedId(NEW_KEY_ID).makePrimary())
                .build();
    }

    private static void act5Envelope(StringBuilder out) throws GeneralSecurityException {
        line(out, "FIVE. Keys that stay at home: envelope encryption.");
        CountingAead master = new CountingAead(new Bank(DemoKeysets.aes256Gcm(MASTER_ID, MASTER_TEXT)).aead());
        Aead envelope = envelope(master);
        line(out, "  BEFORE ENCRYPTION:");
        line(out, "    plain text   " + new String(LINE, StandardCharsets.UTF_8).trim() + "   (" + LINE.length + " bytes)");
        line(out, "    data key     32 random bytes, new for every message");
        line(out, "    master key   key id " + MASTER_ID + ", standing in for a KMS");
        byte[] first = envelope.encrypt(LINE, ACCOUNT);
        byte[] second = envelope.encrypt(LINE, ACCOUNT);
        byte[] wrapped = wrappedKey(first);
        int body = first.length - 4 - wrapped.length;
        line(out, "  AFTER ENCRYPTION (" + LINE.length + " bytes become " + first.length + "):");
        line(out, "    length       " + HexFormat.of().formatHex(first, 0, 4) + " = " + wrapped.length
                + ", the wrapped key's size");
        line(out, "    wrapped key  " + wrapped.length + " = 5 + 12 + 32 + 16, starts " + start(wrapped));
        line(out, "    cipher text  " + body + " = 12 + 16 + 16, no key id");
        line(out, "  4 + " + wrapped.length + " + " + body + " = " + first.length + " bytes");
        byte[] statement = new byte[100_000];
        byte[] big = envelope.encrypt(statement, ACCOUNT);
        line(out, "  two messages, different wrapped keys: " + !Arrays.equals(wrapped, wrappedKey(second)));
        line(out, "  master key calls: " + master.calls() + ", one per message, any size");
        line(out, "  opened: " + text(envelope.decrypt(first, ACCOUNT)) + ", and "
                + envelope.decrypt(big, ACCOUNT).length + " bytes");
        Aead wrong = envelope(new Bank(DemoKeysets.aes256Gcm(MASTER_ID, "a-different-master-key-text-here")).aead());
        String result;
        try {
            wrong.decrypt(first, ACCOUNT);
            result = "opened";
        } catch (GeneralSecurityException e) {
            result = "refused";
        }
        line(out, "  another master key: " + result);
        line(out, "  master key's bytes in a message: " + contains(first, DemoKeysets.keyBytes(MASTER_TEXT)));
    }

    /** Envelope encryption, written out with Tink primitives: a fresh data key for each message, wrapped by the master key. */
    static Aead envelope(Aead master) {
        return new EnvelopeAead(master);
    }

    /** The wrapped data key inside an envelope message: a 4-byte length, then that many bytes. */
    static byte[] wrappedKey(byte[] message) {
        int length = ByteBuffer.wrap(message).getInt();
        return Arrays.copyOfRange(message, 4, 4 + length);
    }

    private static boolean contains(byte[] haystack, byte[] needle) {
        for (int i = 0; i + needle.length <= haystack.length; i++) {
            if (Arrays.equals(Arrays.copyOfRange(haystack, i, i + needle.length), needle)) {
                return true;
            }
        }
        return false;
    }

    private static void act6Bill(StringBuilder out) {
        line(out, "SIX. The bill: what Tink decides, and what it does not.");
        StringBuilder available = new StringBuilder();
        for (String name : new String[] {"AES128_GCM", "AES256_GCM", "CHACHA20_POLY1305", "XCHACHA20_POLY1305"}) {
            if (exists(name)) {
                available.append(available.length() == 0 ? "" : ", ").append(name);
            }
        }
        line(out, "  THE MENU (AEAD templates Tink offers, among others):");
        line(out, "    " + available.substring(0, available.indexOf(", CHACHA")) + ",");
        line(out, "    " + available.substring(available.indexOf("CHACHA")));
        line(out, "    an ECB template: " + (exists("AES128_ECB") ? "offered" : "not offered") + "  <- NOT ON THE MENU");
        Bank bank = new Bank(DemoKeysets.aes256Gcm(KEY_ID, KEY_TEXT));
        int small = bank.seal(ACCOUNT, LINE).length;
        int large = bank.seal(ACCOUNT, new byte[100_000]).length;
        line(out, "  WHAT IT LEAVES TO YOU:");
        line(out, "    " + LINE.length + " -> " + small + " bytes, 100000 -> " + large + ": always " + (small - LINE.length)
                + " more");
        line(out, "    the format is Tink's own: other libraries need Tink");
        line(out, "    a library to add, with its protocol buffer dependency");
        line(out, "    the master key still has to live somewhere");
    }

    private static boolean exists(String template) {
        try {
            KeyTemplates.get(template);
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }
}
