package com.jk.explore.security.secretscan;

import java.util.List;

/** A string put in the bank's repository on purpose, with the truth about it: a real secret, or a look-alike that is not one. */
public record Planted(String id, String value, boolean real) {

    static final Planted STRIPE = new Planted("payment key", "sk_" + "live_51Hq3XkLmN8pRtVwYz2AbCdEf", true);
    static final Planted PASSWORD = new Planted("database password", "Summer2024!", true);
    static final Planted AWS = new Planted("cloud access key", "AK" + "IAQ3RT7KJH2WNP5XCV", true);
    static final Planted GITHUB = new Planted("source token", "ghp" + "_16C7e42F292c6912E7710c838347Ae178B4a", true);
    static final Planted BASE64 = new Planted("key inside a Kubernetes Secret", "sk_" + "live_51Zp9WqJdR4mTxVb7CsUeHgKn", true);
    static final Planted AWS_DOC = new Planted("key in the documentation", "AK" + "IAIOSFODNN7EXAMPLE", false);
    static final Planted HASH = new Planted("checksum", "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", false);
    static final Planted TEST_KEY = new Planted("test key of a fixture", "sk_" + "test_51AbCdEfGhIjKlMnOpQrStUv", false);
    static final Planted UUID = new Planted("identifier", "3f2b8c1e-7a4d-4e5f-9b6a-1c2d3e4f5a6b", false);

    static final List<Planted> ALL = List.of(STRIPE, PASSWORD, AWS, GITHUB, BASE64, AWS_DOC, HASH, TEST_KEY, UUID);

    static List<Planted> realOnes() {
        return ALL.stream().filter(Planted::real).toList();
    }

    /** True when the text a scanner reported contains this value, as it is or as base64. */
    boolean in(String text) {
        return text.contains(value) || text.contains(java.util.Base64.getEncoder().encodeToString(value.getBytes()));
    }
}
