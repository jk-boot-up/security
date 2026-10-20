package com.jk.explore.security.apisec;

/** What the API shows of an account: only what the customer's app needs, with the card number masked. */
public record AccountView(String id, String displayName, String email, long balanceCents, String cardLast4) {

    static AccountView of(Account a) {
        String card = String.valueOf(a.get("cardNumber"));
        return new AccountView(a.id(), String.valueOf(a.get("displayName")), String.valueOf(a.get("email")),
                (Long) a.get("balanceCents"), card.substring(card.length() - 4));
    }
}
