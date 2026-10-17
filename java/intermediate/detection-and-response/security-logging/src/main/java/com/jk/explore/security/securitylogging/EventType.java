package com.jk.explore.security.securitylogging;

import java.util.Locale;

/** The kinds of event a bank must be able to answer questions about later. */
enum EventType {
    LOGIN,
    ACCESS_DECISION,
    MONEY_MOVEMENT,
    ADMIN_CHANGE;

    String label() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
