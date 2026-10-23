package com.jk.explore.security.cryptoagility;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class CryptoAgilityDemo {

    static final int RECORDS = 10_000;

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String yes(boolean b) {
        return b ? "yes" : "no";
    }

    static long count(List<Inventory.Use> uses, Status s) {
        return uses.stream().filter(u -> u.status() == s).count();
    }

    static boolean refuses(Envelope envelope, byte[] blob, Envelope.Rules rules) {
        try {
            envelope.open(blob, rules);
            return false;
        } catch (GeneralSecurityException e) {
            return true;
        }
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();

        out.append("--- what the code asks for\n");
        List<Inventory.Use> uses = Inventory.scan(Path.of("fixtures/bank-src"));
        out.append("algorithm names in the source: " + uses.size() + " uses in " + uses.stream().map(Inventory.Use::file).distinct().count() + " files\n");
        out.append("approved " + count(uses, Status.APPROVED) + ", to migrate " + count(uses, Status.DEPRECATED) + ", forbidden " + count(uses, Status.FORBIDDEN) + ", not on the list " + count(uses, Status.UNKNOWN) + "\n");
        out.append("files with a forbidden algorithm: " + new TreeSet<>(uses.stream().filter(u -> u.status() == Status.FORBIDDEN).map(Inventory.Use::file).toList()) + "\n");

        KeyRing keys = new KeyRing();
        Envelope envelope = new Envelope(keys);
        Store store = new Store(envelope);
        int key1 = keys.add();
        Envelope.Rules legacy = new Envelope.Rules(Set.of(Envelope.V1_CBC_HMAC), Envelope.V1_CBC_HMAC);
        for (int id = 1; id <= RECORDS; id++) {
            store.put(id, Envelope.V1_CBC_HMAC, key1);
        }
        byte[][] backup = new byte[100][];
        for (int i = 0; i < 100; i++) {
            backup[i] = store.raw(i + 1).clone();
        }

        out.append("--- a value that says how it was protected\n");
        out.append("records written by the old system: " + store.census() + "\n");
        int key2 = keys.add();
        Envelope.Rules rules2 = new Envelope.Rules(Set.of(1, 2, 3), Envelope.V2_GCM);
        for (int id = RECORDS + 1; id <= RECORDS + 100; id++) {
            store.put(id, Envelope.V2_GCM, key2);
        }
        out.append("after the bank starts writing AES-GCM under a new key: " + store.census() + "\n");
        out.append("the old reader, which assumes the first suite, refuses a new record: " + yes(refuses(envelope, store.raw(RECORDS + 1), legacy)) + "\n");
        out.append("the reader that picks the suite from the header reads: " + store.intact(rules2) + " of " + store.size() + " records\n");

        out.append("--- moving the data\n");
        out.append("retiring key 1 now would strand " + store.count(key1) + " records: " + (store.count(key1) == 0 ? "allowed" : "refused") + "\n");
        for (int id = 1; id <= 2_000; id++) {
            store.read(id, rules2, key2, true);
        }
        out.append("after 2000 reads that rewrite what they read: " + store.census() + "\n");
        int changed = store.migrateAll(rules2, key2);
        out.append("a batch for the rest changed " + changed + " records: " + store.census() + "\n");
        out.append("records that still read back as written: " + store.intact(rules2) + " of " + store.size() + "\n");

        out.append("--- the algorithm changes again\n");
        int key3 = keys.add();
        Envelope.Rules rules3 = new Envelope.Rules(Set.of(1, 2, 3), Envelope.V3_CHACHA);
        int again = store.migrateAll(rules3, key3);
        out.append("ChaCha20-Poly1305 under a third key: " + again + " records changed, " + store.census() + ", intact " + store.intact(rules3) + " of " + store.size() + "\n");

        out.append("--- retiring a key\n");
        out.append("live records under key 1: " + store.count(key1) + ", key 2: " + store.count(key2) + ", key 3: " + store.count(key3) + "\n");
        out.append("retiring key 1 and key 2: " + (store.count(key1) + store.count(key2) == 0 ? "allowed" : "refused") + "\n");
        keys.retire(key1);
        keys.retire(key2);
        long restorable = Arrays.stream(backup).filter(b -> !refuses(envelope, b, rules3)).count();
        out.append("a backup of 100 records taken before the migration can be read: " + restorable + " of " + backup.length + "\n");

        out.append("--- tampering and downgrade\n");
        byte[] sample = store.raw(5).clone();
        byte[] flipped = sample.clone();
        flipped[1] = Envelope.V2_GCM;
        out.append("the version byte of a stored record changed: refused " + yes(refuses(envelope, flipped, rules3)) + "\n");
        byte[] ecb = sample.clone();
        ecb[1] = Envelope.V0_ECB;
        out.append("a record claiming the unauthenticated first suite: refused by the policy " + yes(refuses(envelope, ecb, rules3)) + "\n");
        Envelope.Rules noV1 = new Envelope.Rules(Set.of(2, 3), Envelope.V3_CHACHA);
        int k = keys.add();
        byte[] old = envelope.seal("customer-0".getBytes(), Envelope.V1_CBC_HMAC, k);
        out.append("a version-1 record after the policy stopped reading version 1: refused " + yes(refuses(envelope, old, noV1)) + "\n");

        out.append("--- the bill\n");
        out.append("agility is a header, a policy and a migration you have practised, not a configuration option\n");
        out.append("a key cannot be retired until the records, the backups and the replicas under it are gone\n");
        out.append("the policy decides what is read and what is written: an old version that is read forever is a downgrade waiting\n");
        return out.toString();
    }
}
