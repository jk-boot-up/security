package com.jk.explore.security.hashingspring;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The five customers of the online store, with the passwords they chose, and the attacker's list of common passwords. */
final class Customers {

    static final Map<String, String> PASSWORDS = new LinkedHashMap<>();

    static {
        PASSWORDS.put("ana", "welcome1");
        PASSWORDS.put("ben", "welcome1");
        PASSWORDS.put("carol", "sunshine");
        PASSWORDS.put("dave", "k9#Tz!vQ2m");
        PASSWORDS.put("erin", "letmein");
    }

    static final List<String> WORDLIST = List.of(
            "123456", "password", "welcome1", "sunshine", "qwerty", "letmein", "iloveyou", "admin");

    private Customers() {
    }
}
