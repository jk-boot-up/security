package com.jk.explore.security.attacks;

/** What a lockout counts failures against: the account as a whole, or each account-and-address pair on its own. */
public enum LockScope {
    ACCOUNT,
    ACCOUNT_AND_ADDRESS
}
