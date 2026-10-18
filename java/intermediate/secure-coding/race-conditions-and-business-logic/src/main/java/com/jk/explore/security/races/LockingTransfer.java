package com.jk.explore.security.races;

/**
 * Moves money between two accounts holding both accounts' locks. Taken in the order "from, then to", two transfers in opposite
 * directions can each hold one lock and wait for the other for ever. Taken in one fixed order for everybody (the smaller account
 * name first), they cannot.
 */
final class LockingTransfer implements Steps {

    private final String name;
    private final Ledger ledger;
    private final String from;
    private final String to;
    private final long amount;
    private final String first;
    private final String second;
    private int next;
    private boolean waiting;
    private String outcome = "";

    LockingTransfer(String name, Ledger ledger, String from, String to, long amount, boolean fixedOrder) {
        this.name = name;
        this.ledger = ledger;
        this.from = from;
        this.to = to;
        this.amount = amount;
        boolean swap = fixedOrder && from.compareTo(to) > 0;
        this.first = swap ? to : from;
        this.second = swap ? from : to;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String step() {
        waiting = false;
        String lock = next == 0 ? first : second;
        switch (next) {
            case 0, 1 -> {
                if (!ledger.tryLock(lock, name)) {
                    waiting = true;
                    return name + " waits for " + lock;
                }
                next++;
                return name + " locks " + lock;
            }
            default -> {
                next++;
                ledger.set(from, ledger.balance(from) - amount);
                ledger.set(to, ledger.balance(to) + amount);
                ledger.unlock(first, name);
                ledger.unlock(second, name);
                outcome = "moved " + amount;
                return name + " moves " + amount + " from " + from + " to " + to + " and unlocks both";
            }
        }
    }

    @Override
    public boolean waiting() {
        return waiting;
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
