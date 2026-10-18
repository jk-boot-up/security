package com.jk.explore.security.asvs;

/** What an application holds and does, which decides how much verification it needs. */
record Profile(String name, Sensitivity data, boolean holdsAccounts, boolean movesMoney, boolean regulated) {

    enum Sensitivity { LOW, MEDIUM, HIGH }
}
