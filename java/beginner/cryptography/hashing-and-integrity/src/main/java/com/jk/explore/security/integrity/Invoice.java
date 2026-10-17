package com.jk.explore.security.integrity;

/** One invoice of the online store: its number, the customer, the item and the amount in dollars. */
public record Invoice(String number, String customer, String item, int amountDollars) {

    /** The invoice written as one line of text, the form that is hashed. Fields are separated by a bar. */
    public String text() {
        return number + "|" + customer + "|" + item + "|" + amountDollars;
    }

    public Invoice withAmount(int newAmount) {
        return new Invoice(number, customer, item, newAmount);
    }
}
