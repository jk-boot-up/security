package com.jk.explore.security.apispring;

/** What a customer may see of their own account: no role, no limit, no risk score, no full card number, no PIN hash. */
record AccountView(String id, String displayName, String email, long balanceCents, String cardLast4) {

    static AccountView of(Account a) {
        String card = a.getCardNumber();
        return new AccountView(a.getId(), a.getDisplayName(), a.getEmail(), a.getBalanceCents(), "****" + card.substring(card.length() - 4));
    }
}
