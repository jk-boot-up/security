package com.jk.explore.security.cryptoagility;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** The bank's records, sealed in envelopes, and the ways of moving them from one suite and key to another. */
public final class Store {

    private final Map<Integer, byte[]> records = new LinkedHashMap<>();
    private final Envelope envelope;

    public Store(Envelope envelope) {
        this.envelope = envelope;
    }

    public static String plainOf(int id) {
        return "customer-" + id + ";balance=" + (id * 37 % 100_000);
    }

    public void put(int id, int version, int keyId) {
        records.put(id, envelope.seal(plainOf(id).getBytes(StandardCharsets.UTF_8), version, keyId));
    }

    public byte[] raw(int id) {
        return records.get(id);
    }

    public void putRaw(int id, byte[] blob) {
        records.put(id, blob);
    }

    public int size() {
        return records.size();
    }

    /** Reads a record; when it is not in the current suite and key it is written again in them (read-repair). */
    public String read(int id, Envelope.Rules rules, int keyId, boolean repair) throws GeneralSecurityException {
        byte[] blob = records.get(id);
        byte[] plain = envelope.open(blob, rules);
        if (repair && (Envelope.versionOf(blob) != rules.writeVersion() || Envelope.keyOf(blob) != keyId)) {
            records.put(id, envelope.seal(plain, rules.writeVersion(), keyId));
        }
        return new String(plain, StandardCharsets.UTF_8);
    }

    /** Rewrites every record that is not in the current suite and key; returns how many it changed. */
    public int migrateAll(Envelope.Rules rules, int keyId) throws GeneralSecurityException {
        int changed = 0;
        for (int id : records.keySet().stream().toList()) {
            byte[] blob = records.get(id);
            if (Envelope.versionOf(blob) != rules.writeVersion() || Envelope.keyOf(blob) != keyId) {
                records.put(id, envelope.seal(envelope.open(blob, rules), rules.writeVersion(), keyId));
                changed++;
            }
        }
        return changed;
    }

    /** How many records there are of each version and key, like "v1/key1". */
    public Map<String, Integer> census() {
        Map<String, Integer> m = new TreeMap<>();
        for (byte[] b : records.values()) {
            m.merge("v" + Envelope.versionOf(b) + "/key" + Envelope.keyOf(b), 1, Integer::sum);
        }
        return m;
    }

    public int count(int keyId) {
        return (int) records.values().stream().filter(b -> Envelope.keyOf(b) == keyId).count();
    }

    /** How many records read back as the text that was put in. */
    public int intact(Envelope.Rules rules) {
        int ok = 0;
        for (Map.Entry<Integer, byte[]> e : records.entrySet()) {
            try {
                if (new String(envelope.open(e.getValue(), rules), StandardCharsets.UTF_8).equals(plainOf(e.getKey()))) {
                    ok++;
                }
            } catch (GeneralSecurityException ex) {
                // not readable under these rules
            }
        }
        return ok;
    }
}
