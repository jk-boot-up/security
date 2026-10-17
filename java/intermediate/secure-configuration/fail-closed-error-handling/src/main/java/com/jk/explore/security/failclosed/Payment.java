package com.jk.explore.security.failclosed;

/** A card payment the bank is asked to approve. Amounts are in whole cents. */
record Payment(String id, String card, String merchant, long cents) {

    /** The card number as it may appear in a log: the last four digits only. */
    String maskedCard() {
        return "**** " + card.substring(card.length() - 4);
    }
}
