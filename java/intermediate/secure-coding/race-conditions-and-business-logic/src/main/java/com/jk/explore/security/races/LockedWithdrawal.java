package com.jk.explore.security.races;

/**
 * The same three steps inside a lock on the account, so they run as one unit: a second request waits until the first has written.
 * The lock's name says whose lock it is; two servers that each lock in their own memory use two different names, and do not stop each other.
 */
final class LockedWithdrawal implements Steps {

    private final String name;
    private final Ledger ledger;
    private final String account;
    private final String lock;
    private final long amount;
    private int next;
    private long seen;
    private boolean approved;
    private boolean waiting;
    private String outcome = "";

    LockedWithdrawal(String name, Ledger ledger, String account, String lock, long amount) {
        this.name = name;
        this.ledger = ledger;
        this.account = account;
        this.lock = lock;
        this.amount = amount;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String step() {
        waiting = false;
        switch (next) {
            case 0 -> {
                if (!ledger.tryLock(lock, name)) {
                    waiting = true;
                    return name + " waits for the lock " + lock;
                }
                next++;
                return name + " takes the lock " + lock;
            }
            case 1 -> {
                next++;
                seen = ledger.balance(account);
                return name + " reads the balance: " + seen;
            }
            case 2 -> {
                next++;
                approved = seen >= amount;
                return name + " checks that " + seen + " covers " + amount + ": " + (approved ? "yes" : "no");
            }
            default -> {
                next++;
                String done;
                if (approved) {
                    ledger.set(account, seen - amount);
                    ledger.payOut(account, amount);
                    outcome = "paid out " + amount;
                    done = name + " writes " + (seen - amount) + ", pays out " + amount;
                } else {
                    outcome = "refused";
                    done = name + " refuses";
                }
                ledger.unlock(lock, name);
                return done + " and releases the lock";
            }
        }
    }

    @Override
    public boolean waiting() {
        return waiting;
    }

    @Override
    public boolean finished() {
        return next >= 4;
    }

    @Override
    public String outcome() {
        return outcome;
    }
}
