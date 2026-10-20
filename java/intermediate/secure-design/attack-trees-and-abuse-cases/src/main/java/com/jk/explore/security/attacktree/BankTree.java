package com.jk.explore.security.attacktree;

import static com.jk.explore.security.attacktree.Node.and;
import static com.jk.explore.security.attacktree.Node.leaf;
import static com.jk.explore.security.attacktree.Node.or;

/** The attack tree for "move money out of Ana's account" at the online bank, as it is before any of the defences below. */
public final class BankTree {

    private BankTree() {
    }

    public static Node baseline() {
        return or("Move money out of Ana's account",
                or("Log in as Ana",
                        and("Phish her password and one-time code",
                                leaf("A1a", "build a phishing page that relays the code", 300, 0.9, "AC1"),
                                leaf("A1b", "she enters the password and the code", 50, 0.20, "AC1")),
                        and("Reuse a leaked password",
                                leaf("A2a", "buy a list of leaked passwords", 100, 0.8, "AC2"),
                                leaf("A2b", "she reuses one, and nothing else is asked", 0, 0.15, "AC2")),
                        and("SIM swap",
                                leaf("A3a", "talk the phone company into a new SIM", 1500, 0.3, "AC3"),
                                leaf("A3b", "know or guess her password", 100, 0.15, "AC3")),
                        and("Steal her session with a script",
                                leaf("A4a", "find a stored script injection in the memo field", 2000, 0.10, "AC4"),
                                leaf("A4b", "she opens the page while signed in", 0, 0.5, "AC4"))),
                or("Abuse the transfer call",
                        leaf("B1", "change the account number in the request to Ana's", 50, 0.9, "AC5"),
                        and("Forge a request from another site",
                                leaf("B2a", "lure Ana to a page with a hidden form", 100, 0.5, "AC6"),
                                leaf("B2b", "the bank accepts a request with only her cookie", 0, 0.95, "AC6"))),
                leaf("C", "bribe an employee who can approve transfers", 20000, 0.2, "AC7"),
                and("Break in through the software",
                        leaf("D1", "exploit a known flaw in an unpatched library", 5000, 0.15, "AC8"),
                        leaf("D2", "move from the web server to the database", 1000, 0.6, "")));
    }
}
