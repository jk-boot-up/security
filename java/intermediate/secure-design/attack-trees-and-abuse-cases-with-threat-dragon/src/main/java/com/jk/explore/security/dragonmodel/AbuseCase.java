package com.jk.explore.security.dragonmodel;

import java.util.List;
import java.util.function.Predicate;

/**
 * "As an attacker I want to ..., so that ...": an abuse case written as a check against the bank system. It passes when the abuse is <em>refused</em>, so a missing
 * defence is a failing test, and the test goes green when the defence is built.
 */
public record AbuseCase(String id, String story, Predicate<BankConfig> refused) {

    public static List<AbuseCase> all() {
        return List.of(
                new AbuseCase("AC1", "As a phisher I replay Ana's password and one-time code from my own device", c -> c.has("passkey")),
                new AbuseCase("AC2", "As a thief with a leaked password I sign in as Ana", c -> c.has("otp") || c.has("passkey") || c.has("breached")),
                new AbuseCase("AC3", "As a SIM swapper I read Ana's text-message code", c -> c.has("otp") || c.has("passkey")),
                new AbuseCase("AC4", "As an attacker I store a script in a memo to steal Ana's session", c -> c.has("csp")),
                new AbuseCase("AC5", "As Mallory I transfer from Ana's account by changing the account number", c -> c.has("ownership")),
                new AbuseCase("AC6", "As an attacker I forge a transfer from another site using Ana's cookie", c -> c.has("csrf")),
                new AbuseCase("AC7", "As an insider I approve my own transfer", c -> c.has("dual")),
                new AbuseCase("AC8", "As an attacker I exploit a library flaw that was fixed last month", c -> c.has("patched")));
    }

    public boolean passes(BankConfig c) {
        return refused.test(c);
    }
}
