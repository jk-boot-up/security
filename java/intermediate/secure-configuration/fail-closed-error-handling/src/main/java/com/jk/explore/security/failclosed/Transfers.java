package com.jk.explore.security.failclosed;

/** Two ways to move money between accounts when the second half can fail. */
final class Transfers {

    private Transfers() {
    }

    /** Debit, then credit. If the credit throws, the debit has already happened and the money is gone. */
    static boolean naive(Ledger ledger, String from, String to, long cents) {
        try {
            ledger.debit(from, cents);
            ledger.credit(to, cents);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * One unit of work: check everything that can be checked first, then make both changes, and if the second still fails, undo the
     * first. Either both halves happen or neither does.
     */
    static boolean atomic(Ledger ledger, String from, String to, long cents) {
        if (cents <= 0 || !ledger.exists(from) || !ledger.acceptsCredit(to) || ledger.balance(from) < cents) {
            return false;
        }
        ledger.debit(from, cents);
        try {
            ledger.credit(to, cents);
            return true;
        } catch (RuntimeException e) {
            ledger.credit(from, cents);
            return false;
        }
    }
}
