package com.jk.explore.security.attacktree;

import java.util.List;
import java.util.function.Predicate;

/**
 * "As an attacker I want to ..., so that ...": an abuse case written as a check against the bank system. It passes when the abuse is <em>refused</em>, so a missing
 * defence is a failing test, and the test goes green when the defence is built.
 */
public record AbuseCase(String id, String story, Predicate<BankConfig> refused) {

    public static List<AbuseCase> all() {
        return List.of(
                new AbuseCase("AC1", "As a phisher I replay Ana's password and code", c -> c.has("passkey")),
                new AbuseCase("AC2", "As a thief I sign in with a leaked password", c -> c.has("otp") || c.has("passkey") || c.has("breached")),
                new AbuseCase("AC3", "As a SIM swapper I read Ana's text code", c -> c.has("otp") || c.has("passkey")),
                new AbuseCase("AC4", "As an attacker I steal her session via a memo", c -> c.has("csp")),
                new AbuseCase("AC5", "As Mallory I change the account number to Ana's", c -> c.has("ownership")),
                new AbuseCase("AC6", "As another site I forge a transfer with her cookie", c -> c.has("csrf")),
                new AbuseCase("AC7", "As an insider I approve my own transfer", c -> c.has("dual")),
                new AbuseCase("AC8", "As an attacker I exploit last month's library flaw", c -> c.has("patched")));
    }

    public boolean passes(BankConfig c) {
        return refused.test(c);
    }
}
