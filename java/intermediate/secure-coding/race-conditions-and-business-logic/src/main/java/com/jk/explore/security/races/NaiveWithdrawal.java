package com.jk.explore.security.races;

/** Check, then act: read the balance, check it, write the new balance. Three steps, and another request can run between them. */
final class NaiveWithdrawal implements Steps {

    private final String name;
    private final Ledger ledger;
    private final String account;
    private final long amount;
    private int next;
    private long seen;
    private boolean approved;
    private String outcome = "";

    NaiveWithdrawal(String name, Ledger ledger, String account, long amount) {
        this.name = name;
        this.ledger = ledger;
        this.account = account;
        this.amount = amount;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String step() {
        switch (next++) {
            case 0 -> {
                seen = ledger.balance(account);
                return name + " reads the balance: " + seen;
            }
            case 1 -> {
                approved = seen >= amount;
                return name + " checks that " + seen + " covers " + amount + ": " + (approved ? "yes" : "no");
            }
            default -> {
                if (!approved) {
                    outcome = "refused";
                    return name + " refuses";
                }
                ledger.set(account, seen - amount);
                ledger.payOut(account, amount);
                outcome = "paid out " + amount;
                return name + " writes " + (seen - amount) + " and pays out " + amount;
            }
        }
    }

    @Override
    public boolean waiting() {
        return false;
    }

    @Override
    public boolean finished() {
        return next >= 3;
    }

    @Override
    public String outcome() {
        return outcome;
    }
}
