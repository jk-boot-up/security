package com.jk.explore.security.detectioncode;

/**
 * What an event really is. kind: benign, lookalike (benign, and a rule without the right filter fires on it), filtered (benign, and the rule has
 * a filter for it), attack (a rule should fire), evasion (an attack the rule is built not to see), uncovered (an attack no rule is written for).
 * rule is the rule meant to catch it and instance numbers one attack (several events for a burst); technique is the ATT&CK id.
 */
record Truth(int id, String kind, String rule, String technique, int instance) {

    boolean benign() {
        return kind.equals("benign") || kind.equals("lookalike") || kind.equals("filtered");
    }

    boolean attack() {
        return !benign();
    }
}
