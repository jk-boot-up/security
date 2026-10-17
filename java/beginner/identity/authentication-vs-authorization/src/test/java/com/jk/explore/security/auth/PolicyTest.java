package com.jk.explore.security.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PolicyTest {

    /** role, user, action, order owner, expected */
    @ParameterizedTest
    @CsvSource({
        "CUSTOMER, ben, VIEW,   ben, true",
        "CUSTOMER, ben, CANCEL, ben, true",
        "CUSTOMER, ben, REFUND, ben, false",
        "CUSTOMER, ben, VIEW,   ana, false",
        "CUSTOMER, ben, CANCEL, ana, false",
        "SUPPORT,  sam, VIEW,   ana, true",
        "SUPPORT,  sam, REFUND, ana, true",
        "SUPPORT,  sam, CANCEL, ana, false",
        "ADMIN,    alex, VIEW,  ana, true",
        "ADMIN,    alex, CANCEL, ana, true",
        "ADMIN,    alex, REFUND, ana, true"
    })
    void theRulesGiveTheExpectedAnswer(Role role, String user, Action action, String owner, boolean expected) {
        Order order = new Order("O-x", owner, "Thing", 10);
        assertEquals(expected, PolicyGate.allowed(role, user, action, order));
    }
}
