package com.jk.explore.security.failclosedspring;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Four ways to write the same transfer, and only one of them is a unit of work. The credit fails when the receiving account is frozen. */
@Service
class Transfers {

    private final Ledger ledger;

    Transfers(Ledger ledger) {
        this.ledger = ledger;
    }

    /** Debit, then credit, each committed on its own. */
    void withoutTransaction(String from, String to, long cents) throws FrozenAccountException {
        ledger.debit(from, cents);
        ledger.credit(to, cents);
    }

    /** Looks safe, and is not: Spring rolls back on unchecked exceptions only, so the checked one commits the debit. */
    @Transactional
    void transactionalButChecked(String from, String to, long cents) throws FrozenAccountException {
        ledger.debit(from, cents);
        ledger.credit(to, cents);
    }

    /** One unit of work: any exception undoes both halves. */
    @Transactional(rollbackFor = Exception.class)
    void unitOfWork(String from, String to, long cents) throws FrozenAccountException {
        ledger.debit(from, cents);
        ledger.credit(to, cents);
    }

    /** Looks safe, and is not: the call to {@code this} does not go through Spring's proxy, so no transaction is ever opened. */
    void callsItsOwnTransactionalMethod(String from, String to, long cents) throws FrozenAccountException {
        this.unitOfWork(from, to, cents);
    }
}
