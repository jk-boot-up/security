package com.jk.explore.security.apisec;

import java.util.LinkedHashMap;
import java.util.Map;

/** A bank account as the database holds it: some fields are the customer's to change, most are not. */
public final class Account {

    private final String id;
    private final String owner;
    private final Map<String, Object> fields = new LinkedHashMap<>();

    public Account(String id, String owner, String displayName, String email, String cardNumber) {
        this.id = id;
        this.owner = owner;
        fields.put("displayName", displayName);
        fields.put("email", email);
        fields.put("role", "customer");
        fields.put("balanceCents", 125_000L);
        fields.put("creditLimitCents", 50_000L);
        fields.put("riskScore", 17L);
        fields.put("cardNumber", cardNumber);
        fields.put("pinHash", "pbkdf2$" + id + "$demo");
    }

    public String id() {
        return id;
    }

    public String owner() {
        return owner;
    }

    public Object get(String field) {
        return fields.get(field);
    }

    public void set(String field, Object value) {
        fields.put(field, value);
    }

    /** Every field, which is what a response built from the entity itself would send. */
    public Map<String, Object> all() {
        return new LinkedHashMap<>(fields);
    }
}
