package com.jk.explore.security.symmetrictink;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.PredefinedAeadParameters;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HexFormat;

/** Runs the online bank story in five numbered acts, with Google's Tink, and prints exact, repeatable results. */
public class TinkDemo {

    static final int KEY_ID = 1001;
    static final String KEY_TEXT = "demo-statement-key-not-for-real";
    static final byte[] ACCOUNT = "account=ANA-1".getBytes(StandardCharsets.UTF_8);
    static final String MASTER_TEXT = "demo-master-key-standing-for-a-kms";
    static final byte[] LINE = "RENT      -1200\n".getBytes(StandardCharsets.UTF_8);

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        try {
            act1NoNonce(out);
            act2Tamper(out);
            act3Rotation(out);
            act4Envelope(out);
            act5Bill(out);
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

    private static void act1NoNonce(StringBuilder out) {
        line(out, "ONE. One call, and no nonce to look after.");
        Bank bank = new Bank(DemoKeysets.aes256Gcm(KEY_ID, KEY_TEXT));
        byte[] first = bank.seal(ACCOUNT, LINE);
        byte[] second = bank.seal(ACCOUNT, LINE);
        line(out, "  the same line sealed twice gives the same bytes: " + Arrays.equals(first, second) + "  <- TINK PICKS A NEW NONCE");
        line(out, "  the stored message starts with " + HexFormat.of().formatHex(first, 0, 5) + ": version 1, then key id " + KEY_ID);
        line(out, "  " + LINE.length + " bytes seal to " + first.length + " bytes: 5 for the key id, 12 for the nonce, 16 for the tag");
        line(out, "  opened with the right key and account: " + text(bank.openOrNull(ACCOUNT, first)));
    }

    private static void act2Tamper(StringBuilder out) {
        line(out, "TWO. Any change is refused.");
        Bank bank = new Bank(DemoKeysets.aes256Gcm(KEY_ID, KEY_TEXT));
        byte[] sealed = bank.seal(ACCOUNT, LINE);
        byte[] flipped = sealed.clone();
        flipped[sealed.length - 1] ^= 0x01;
        line(out, "  one bit of the message is flipped: " + text(bank.openOrNull(ACCOUNT, flipped)) + "  <- DETECTED");
        line(out, "  the message is moved to another account: "
                + text(bank.openOrNull("account=MAL-2".getBytes(StandardCharsets.UTF_8), sealed)) + "  <- DETECTED");
        Bank other = new Bank(DemoKeysets.aes256Gcm(KEY_ID, "a-completely-different-key-text!"));
        line(out, "  opened with a different key of the same id: " + text(other.openOrNull(ACCOUNT, sealed)));
    }

    private static void act3Rotation(StringBuilder out) throws GeneralSecurityException {
        line(out, "THREE. Replacing the key without losing the data.");
        KeysetHandle original = DemoKeysets.aes256Gcm(KEY_ID, KEY_TEXT);
        byte[] old = new Bank(original).seal(ACCOUNT, LINE);
        KeysetHandle rotated = KeysetHandle.newBuilder(original)
                .addEntry(KeysetHandle.generateEntryFromParameters(PredefinedAeadParameters.AES256_GCM).withRandomId().makePrimary())
                .build();
        Bank bank = new Bank(rotated);
        byte[] fresh = bank.seal(ACCOUNT, LINE);
        line(out, "  a new key is added and made primary: the keyset now holds " + rotated.size() + " keys");
        line(out, "  a message sealed before the change still opens: " + text(bank.openOrNull(ACCOUNT, old)));
        line(out, "  a new message starts with a different key id: " + !Arrays.equals(Arrays.copyOf(old, 5), Arrays.copyOf(fresh, 5)));
        KeysetHandle onlyNew = KeysetHandle.newBuilder(rotated).deleteAt(0).build();
        line(out, "  the old key is removed from the keyset: " + text(new Bank(onlyNew).openOrNull(ACCOUNT, old))
                + " for old messages, " + text(new Bank(onlyNew).openOrNull(ACCOUNT, fresh)) + " for new ones");
    }

    private static void act4Envelope(StringBuilder out) throws GeneralSecurityException {
        line(out, "FOUR. Keys that stay at home: envelope encryption.");
        CountingAead master = new CountingAead(new Bank(DemoKeysets.aes256Gcm(2001, MASTER_TEXT)).aead());
        Aead envelope = envelope(master);
        byte[] first = envelope.encrypt(LINE, ACCOUNT);
        byte[] second = envelope.encrypt(LINE, ACCOUNT);
        byte[] statement = new byte[100_000];
        byte[] big = envelope.encrypt(statement, ACCOUNT);
        line(out, "  each message is sealed with its own fresh data key, and only that small key goes to the master key to be wrapped");
        line(out, "  " + LINE.length + " bytes become " + first.length + ": a length, the wrapped data key, then the cipher text");
        line(out, "  two messages carry different wrapped keys: " + !Arrays.equals(wrappedKey(first), wrappedKey(second)));
        line(out, "  the master key was used " + master.calls() + " times for 2 short lines and a " + statement.length
                + "-byte statement: once each, whatever the size");
        line(out, "  opened with the master key: " + text(envelope.decrypt(first, ACCOUNT)) + ", and " + envelope.decrypt(big, ACCOUNT).length + " bytes");
        Aead wrong = envelope(new Bank(DemoKeysets.aes256Gcm(2001, "a-different-master-key-text-here")).aead());
        String result;
        try {
            wrong.decrypt(first, ACCOUNT);
            result = "opened";
        } catch (GeneralSecurityException e) {
            result = "refused";
        }
        line(out, "  opened with a different master key: " + result);
        line(out, "  the master key's own bytes appear in a stored message: " + contains(first, DemoKeysets.keyBytes(MASTER_TEXT)));
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

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: what Tink decides for you, and what it does not.");
        StringBuilder available = new StringBuilder();
        for (String name : new String[] {"AES128_GCM", "AES256_GCM", "AES128_EAX", "AES256_CTR_HMAC_SHA256",
            "CHACHA20_POLY1305", "XCHACHA20_POLY1305", "AES256_GCM_SIV"}) {
            if (exists(name)) {
                available.append(available.length() == 0 ? "" : ", ").append(name);
            }
        }
        line(out, "  the AEAD templates offered include: " + available);
        line(out, "  an ECB template: " + (exists("AES128_ECB") ? "offered" : "not offered") + "  <- THE UNSAFE CHOICE IS NOT ON THE MENU");
        line(out, "  the stored format is Tink's own: other libraries need Tink to read it");
        line(out, "  Tink adds a library and its protocol buffer dependency, and the master key still has to live somewhere");
        line(out, "  and message lengths still show: 33 bytes more than the message, every time");
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
