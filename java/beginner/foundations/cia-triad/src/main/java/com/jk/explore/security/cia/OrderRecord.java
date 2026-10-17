package com.jk.explore.security.cia;

/** Everything the store keeps about one order: customer, item, shipping address and amount paid. */
public record OrderRecord(String id, String customer, String item, String address, int amountDollars) {

    OrderRecord withAmount(int newAmountDollars) {
        return new OrderRecord(id, customer, item, address, newAmountDollars);
    }

    /** The text the integrity seal is computed over, so any changed field changes the seal. */
    String sealedText() {
        return String.join("|", id, customer, item, address, Integer.toString(amountDollars));
    }
}
