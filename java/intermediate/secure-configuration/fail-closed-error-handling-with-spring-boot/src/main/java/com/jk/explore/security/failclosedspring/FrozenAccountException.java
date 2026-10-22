package com.jk.explore.security.failclosedspring;

/** A checked exception, which is exactly why a plain {@code @Transactional} does not undo the work before it. */
class FrozenAccountException extends Exception {

    FrozenAccountException(String account) {
        super("account " + account + " is frozen");
    }
}
