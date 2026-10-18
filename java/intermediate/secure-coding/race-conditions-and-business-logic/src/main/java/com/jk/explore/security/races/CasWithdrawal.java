package com.jk.explore.security.races;

/**
 * No lock: read, check, then write with a compare-and-set that succeeds only if the balance is still the value that was read.
 * If another request wrote in between, the write fails and the request starts again from a fresh read.
 */
final class CasWithdrawal implements Steps {

    private enum Phase { READ, CHECK, WRITE, DONE }

    private final String name;
    private final Ledger ledger;
    private final String account;
    private final long amount;
    private Phase phase = Phase.READ;
    private long seen;
    private String outcome = "";

    CasWithdrawal(String name, Ledger ledger, String account, long amount) {
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
        switch (phase) {
            case READ -> {
                seen = ledger.balance(account);
                phase = Phase.CHECK;
                return name + " reads the balance: " + seen;
            }
            case CHECK -> {
                if (seen < amount) {
                    phase = Phase.DONE;
                    outcome = "refused";
                    return name + " checks that " + seen + " covers " + amount + ": no, refused";
                }
                phase = Phase.WRITE;
                return name + " checks that " + seen + " covers " + amount + ": yes";
            }
            case WRITE -> {
                if (ledger.compareAndSet(account, seen, seen - amount)) {
                    ledger.payOut(account, amount);
                    phase = Phase.DONE;
                    outcome = "paid out " + amount;
                    return name + " sets " + seen + " to " + (seen - amount) + " and pays out " + amount;
                }
                phase = Phase.READ;
                return name + " tries to set " + seen + " to " + (seen - amount) + ": the balance changed, start again";
            }
            default -> throw new IllegalStateException("finished");
        }
    }

    @Override
    public boolean waiting() {
        return false;
    }

    @Override
    public boolean finished() {
        return phase == Phase.DONE;
    }

    @Override
    public String outcome() {
        return outcome;
    }
}
