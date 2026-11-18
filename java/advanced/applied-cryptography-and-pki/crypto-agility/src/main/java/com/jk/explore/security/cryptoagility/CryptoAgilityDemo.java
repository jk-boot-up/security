package com.jk.explore.security.cryptoagility;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.crypto.AEADBadTagException;

/** Runs the five demos and prints them in sections: the input of each step, then its result. Only counts and lengths are printed, never keys or ciphertext. */
public class CryptoAgilityDemo {

    static final int RECORDS = 10_000;
    static final int NEW_RECORDS = 100;
    static final int REPAIRED = 2_000;
    static final int BACKUP = 100;

    static final Map<Integer, String> SUITE = Map.of(
            Envelope.V0_ECB, "AES-ECB, no tag",
            Envelope.V1_CBC_HMAC, "AES-CBC + HMAC-SHA256",
            Envelope.V2_GCM, "AES-GCM",
            Envelope.V3_CHACHA, "ChaCha20-Poly1305");

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** Why an envelope was refused, or "read" when it opened. */
    static String outcome(Envelope envelope, byte[] blob, Envelope.Rules rules) {
        try {
            envelope.open(blob, rules);
            return "read";
        } catch (AEADBadTagException e) {
            return "refused: the tag does not match";
        } catch (GeneralSecurityException e) {
            return "refused: " + e.getMessage();
        }
    }

    static void census(StringBuilder out, Store store) {
        store.census().forEach((k, n) -> out.append(String.format("    %-9s %d records%n", k, n)));
    }

    static String status(Status s) {
        return switch (s) {
            case APPROVED -> "approved";
            case DEPRECATED -> "to migrate";
            case FORBIDDEN -> "forbidden";
            case UNKNOWN -> "not on the list";
        };
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();

        out.append("ONE. What the code asks for.\n");
        List<Inventory.Use> uses = Inventory.scan(Path.of("fixtures/bank-src"));
        Map<String, List<String>> byFile = new LinkedHashMap<>();
        uses.forEach(u -> byFile.computeIfAbsent(u.file(), f -> new java.util.ArrayList<>()).add(u.algorithm()));
        out.append("  THE BANK'S SOURCE (each file, the names it asks getInstance for):\n");
        byFile.forEach((f, names) -> out.append(String.format("    %-16s %s%n", f.replace(".java", ""), String.join(", ", names))));
        out.append("  ---\n");
        out.append("  THE INVENTORY, EACH NAME JUDGED BY THE POLICY:\n");
        Map<String, Long> perName = uses.stream().collect(Collectors.groupingBy(Inventory.Use::algorithm, Collectors.counting()));
        perName.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<String, Long> e) -> Policy.of(e.getKey())).thenComparing(e -> -e.getValue()).thenComparing(Map.Entry::getKey))
                .forEach(e -> out.append(String.format("    %-24s x%d  %s%n", e.getKey(), e.getValue(), status(Policy.of(e.getKey())))));
        out.append("  ---\n");
        out.append("  THE COUNT:\n");
        out.append(String.format("    uses             %d in %d files%n", uses.size(), byFile.size()));
        for (Status s : Status.values()) {
            out.append(String.format("    %-16s %d%n", status(s), uses.stream().filter(u -> u.status() == s).count()));
        }
        out.append("  FILES TO FIX FIRST (a forbidden name):\n");
        out.append("    " + String.join(", ", new TreeSet<>(uses.stream().filter(u -> u.status() == Status.FORBIDDEN).map(Inventory.Use::file).toList())) + "\n");

        KeyRing keys = new KeyRing();
        Envelope envelope = new Envelope(keys);
        Store store = new Store(envelope);
        int key1 = keys.add();
        Envelope.Rules legacy = new Envelope.Rules(Set.of(Envelope.V1_CBC_HMAC), Envelope.V1_CBC_HMAC);
        for (int id = 1; id <= RECORDS; id++) {
            store.put(id, Envelope.V1_CBC_HMAC, key1);
        }
        byte[][] backup = new byte[BACKUP][];
        for (int i = 0; i < BACKUP; i++) {
            backup[i] = store.raw(i + 1).clone();
        }

        out.append("TWO. A value that says how it was protected.\n");
        String text = Store.plainOf(1);
        int textBytes = text.getBytes(StandardCharsets.UTF_8).length;
        out.append("  ONE RECORD, BEFORE SEALING:\n");
        out.append(String.format("    %s (%d bytes)%n", text, textBytes));
        out.append("  THE SAME RECORD, SEALED IN EACH SUITE (bytes):\n");
        int key2 = keys.add();
        for (int v : new int[] {Envelope.V1_CBC_HMAC, Envelope.V2_GCM, Envelope.V3_CHACHA}) {
            Layout l = Layout.of(envelope.seal(text.getBytes(StandardCharsets.UTF_8), v, v == 1 ? key1 : key2));
            out.append(String.format("    v%d %-22s %d + %d + %d + %d = %d%n", v, SUITE.get(v), Layout.HEADER, l.iv(), l.ciphertext(), l.tag(), l.total()));
        }
        out.append(String.format("    (header + IV + ciphertext + tag; CBC pads %d to %d)%n", textBytes, Layout.cbcLength(textBytes)));
        out.append("  HOW THE HEADER IS MADE (3 bytes, in hex):\n");
        out.append(String.format("    magic    %02x  the letter '%c': this is an envelope%n", (int) Envelope.MAGIC, (char) Envelope.MAGIC));
        out.append("    version  01 02 03  which suite sealed it\n");
        out.append("    key id   01 02 03  which key sealed it\n");
        out.append("    the tag covers the header too: it cannot be changed\n");
        out.append("  ---\n");
        out.append("  THE STORE, WRITTEN BY THE OLD SYSTEM:\n");
        census(out, store);
        Envelope.Rules rules2 = new Envelope.Rules(Set.of(1, 2, 3), Envelope.V2_GCM);
        for (int id = RECORDS + 1; id <= RECORDS + NEW_RECORDS; id++) {
            store.put(id, Envelope.V2_GCM, key2);
        }
        out.append(String.format("  THE STORE, AFTER %d NEW RECORDS IN AES-GCM UNDER KEY 2:%n", NEW_RECORDS));
        census(out, store);
        out.append(String.format("  TWO READERS, ONE NEW RECORD (header %s):%n", Layout.of(store.raw(RECORDS + 1)).header()));
        out.append("    old reader, assumes v1      " + outcome(envelope, store.raw(RECORDS + 1), legacy).replace("version 2 is not accepted by the policy", "it knows only v1") + "\n");
        out.append(String.format("    reader that follows header  reads %d of %d%n", store.intact(rules2), store.size()));

        out.append("THREE. Moving the data.\n");
        out.append("  BEFORE THE MOVE:\n");
        census(out, store);
        int stranded = store.count(key1);
        out.append(String.format("    retire key 1 now?  refused, %d records need it%n", stranded));
        for (int id = 1; id <= REPAIRED; id++) {
            store.read(id, rules2, key2, true);
        }
        out.append(String.format("  STEP 1, %d READS THAT REWRITE WHAT THEY READ:%n", REPAIRED));
        census(out, store);
        int left = store.count(key1);
        int changed = store.migrateAll(rules2, key2);
        out.append("  STEP 2, A BATCH FOR THE REST:\n");
        out.append(String.format("    changed   %d records%n", changed));
        census(out, store);
        out.append("  ---\n");
        out.append("  HOW THE COUNTS ADD UP:\n");
        out.append(String.format("    key 1  %d - %d = %d, then - %d = %d%n", stranded, REPAIRED, left, changed, store.count(key1)));
        out.append(String.format("    key 2  %d + %d = %d, then + %d = %d%n", NEW_RECORDS, REPAIRED, NEW_RECORDS + REPAIRED, changed, store.count(key2)));
        out.append("  AFTER THE MOVE:\n");
        out.append(String.format("    read back as written  %d of %d%n", store.intact(rules2), store.size()));

        out.append("FOUR. The next rotation, and retiring a key.\n");
        int key3 = keys.add();
        Envelope.Rules rules3 = new Envelope.Rules(Set.of(1, 2, 3), Envelope.V3_CHACHA);
        out.append("  BEFORE THE ROTATION:\n");
        census(out, store);
        int again = store.migrateAll(rules3, key3);
        out.append("  AFTER A BATCH TO ChaCha20-Poly1305 UNDER KEY 3:\n");
        out.append(String.format("    changed   %d records%n", again));
        census(out, store);
        out.append(String.format("    read back as written  %d of %d%n", store.intact(rules3), store.size()));
        out.append("  LIVE RECORDS PER KEY:\n");
        for (int k : new int[] {key1, key2, key3}) {
            out.append(String.format("    key %d  %d%n", k, store.count(k)));
        }
        out.append("    retire key 1 and key 2?  " + (store.count(key1) + store.count(key2) == 0 ? "allowed, no live record needs them" : "refused") + "\n");
        out.append("  ---\n");
        Layout old = Layout.of(backup[0]);
        out.append(String.format("  THE BACKUP, TAKEN BEFORE THE FIRST MOVE:%n    %d records, header %s (v1/key1)%n", BACKUP, old.header()));
        out.append(String.format("    readable while key 1 lives  %d of %d%n", Arrays.stream(backup).filter(b -> outcome(envelope, b, rules3).equals("read")).count(), BACKUP));
        keys.retire(key1);
        keys.retire(key2);
        long restorable = Arrays.stream(backup).filter(b -> outcome(envelope, b, rules3).equals("read")).count();
        out.append("  THE BACKUP, AFTER KEY 1 IS RETIRED:\n");
        out.append(String.format("    readable  %d of %d%n", restorable, BACKUP));
        out.append("    first record  " + outcome(envelope, backup[0], rules3) + "\n");

        out.append("FIVE. Tampering and downgrade.\n");
        byte[] sample = store.raw(5).clone();
        Layout five = Layout.of(sample);
        out.append("  THE RECORD, AS STORED:\n");
        out.append(String.format("    record 5  header %s, %d bytes: %s%n", five.header(), five.total(), outcome(envelope, sample, rules3)));
        out.append("  THE POLICY:\n");
        out.append("    reads v1 v2 v3, writes v3\n");
        out.append("    after the cut-off: reads v2 v3, writes v3\n");
        byte[] flipped = sample.clone();
        flipped[1] = Envelope.V2_GCM;
        byte[] ecb = sample.clone();
        ecb[1] = Envelope.V0_ECB;
        Envelope.Rules noV1 = new Envelope.Rules(Set.of(2, 3), Envelope.V3_CHACHA);
        int k = keys.add();
        byte[] oldRecord = envelope.seal(Store.plainOf(0).getBytes(StandardCharsets.UTF_8), Envelope.V1_CBC_HMAC, k);
        out.append("  THREE CHANGES, BEFORE THE READ:\n");
        out.append(String.format("    1  header %s, claims AES-GCM%n", Layout.of(flipped).header()));
        out.append(String.format("    2  header %s, claims AES-ECB, no tag%n", Layout.of(ecb).header()));
        out.append(String.format("    3  header %s, read after the cut-off%n", Layout.of(oldRecord).header()));
        out.append("  THE RESULT:\n");
        out.append("    1  " + outcome(envelope, flipped, rules3) + "\n");
        out.append("    2  " + outcome(envelope, ecb, rules3) + "\n");
        out.append("    3  " + outcome(envelope, oldRecord, noV1) + "\n");
        return out.toString();
    }
}
