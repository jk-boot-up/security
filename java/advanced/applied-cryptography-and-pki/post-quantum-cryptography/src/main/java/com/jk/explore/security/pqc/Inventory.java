package com.jk.explore.security.pqc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * An inventory of the cryptography in a bank's source files: which algorithm each file names, what it is used for,
 * and so in which order the files have to move to post-quantum algorithms.
 * The sources are a fixed sample (BANK_SOURCES), so the inventory gives the same answer on every run.
 */
final class Inventory {

    /** What an algorithm is used for, in migration order: what a recording exposes first. */
    enum Use {
        KEY_EXCHANGE("key exchange"),
        SIGNATURE("signature"),
        SYMMETRIC("symmetric");

        final String label;

        Use(String label) {
            this.label = label;
        }

        int priority() {
            return ordinal() + 1;
        }
    }

    /** An algorithm, the text that names it in source code, its use, and whether a large quantum computer breaks it (Shor). */
    record Algorithm(String name, Pattern pattern, Use use, boolean brokenByShor) {
    }

    static final List<Algorithm> ALGORITHMS = List.of(
            new Algorithm("RSA", Pattern.compile("\"RSA/"), Use.KEY_EXCHANGE, true),
            new Algorithm("ECDH", Pattern.compile("\"ECDH\""), Use.KEY_EXCHANGE, true),
            new Algorithm("X25519", Pattern.compile("\"X25519\""), Use.KEY_EXCHANGE, true),
            new Algorithm("RSA", Pattern.compile("withRSA\""), Use.SIGNATURE, true),
            new Algorithm("ECDSA", Pattern.compile("withECDSA\""), Use.SIGNATURE, true),
            new Algorithm("Ed25519", Pattern.compile("\"Ed25519\""), Use.SIGNATURE, true),
            new Algorithm("AES", Pattern.compile("\"AES/"), Use.SYMMETRIC, false),
            new Algorithm("HMAC", Pattern.compile("\"Hmac"), Use.SYMMETRIC, false));

    /** One source file of the sample: where it is and the lines that matter. */
    record Source(String path, String code) {
    }

    /** A fixed sample of a bank's code: one or two lines per service, as they would appear in its source. */
    static final List<Source> BANK_SOURCES = List.of(
            new Source("online-banking/TlsSetup", "params.setNamedGroups(new String[] {\"X25519\"});"),
            new Source("payments/CardVault", "Cipher.getInstance(\"AES/GCM/NoPadding\"); Mac.getInstance(\"HmacSHA256\");"),
            new Source("statements/StatementSigner", "Signature.getInstance(\"SHA256withRSA\");"),
            new Source("interbank/SettlementChannel", "KeyAgreement.getInstance(\"ECDH\"); Cipher.getInstance(\"AES/GCM/NoPadding\");"),
            new Source("archive/ArchiveKeyWrap", "Cipher.getInstance(\"RSA/ECB/OAEPWithSHA-256AndMGF1Padding\");"),
            new Source("mobile/DeviceBinding", "Signature.getInstance(\"SHA256withECDSA\");"),
            new Source("identity/LoginTokens", "Signature.getInstance(\"Ed25519\"); Mac.getInstance(\"HmacSHA256\");"));

    /** What one file names, and the use that comes first in the migration order. */
    record Entry(String path, List<Algorithm> found, Use first) {
    }

    private Inventory() {
    }

    static List<Entry> scan(List<Source> sources) {
        List<Entry> entries = new ArrayList<>();
        for (Source s : sources) {
            List<Algorithm> found = ALGORITHMS.stream().filter(a -> a.pattern().matcher(s.code()).find()).toList();
            Use first = found.stream().map(Algorithm::use)
                    .min(Enum::compareTo).orElse(Use.SYMMETRIC);
            entries.add(new Entry(s.path(), found, first));
        }
        return entries;
    }

    static long count(List<Entry> entries, Use use) {
        return entries.stream().filter(e -> e.first() == use).count();
    }
}
