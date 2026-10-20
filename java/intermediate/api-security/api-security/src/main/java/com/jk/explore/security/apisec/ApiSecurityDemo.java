package com.jk.explore.security.apisec;

import java.util.List;
import java.util.Map;

/** Runs the bank API story in five numbered acts and prints exact, repeatable results. */
public class ApiSecurityDemo {

    static final Map<String, Object> ESCALATION = Map.of("displayName", "Ana", "role", "admin", "creditLimitCents", 100_000_000L);

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();

        out.append("== 1. A profile call that works ==\n");
        Bank bank = Bank.demo();
        Api naive = new NaiveApi(bank);
        out.append("PATCH displayName            -> ").append(summary(naive.patchProfile("ana", "acc-0001", Map.of("displayName", "Ana Ruiz")))).append('\n');

        out.append("\n== 2. Whatever the caller sends becomes a field ==\n");
        out.append("PATCH role, creditLimit      -> ").append(summary(naive.patchProfile("ana", "acc-0001", ESCALATION))).append("  <- NOT HERS TO CHANGE\n");

        out.append("\n== 3. The response is the whole record, and a list has no end ==\n");
        out.append("GET /accounts/acc-0001       -> ").append(summary(naive.getAccount("ana", "acc-0001"))).append('\n');
        out.append("GET /accounts?size=1000000   -> ").append(summary(naive.listAccounts("ana", 0, 1_000_000))).append("  <- EVERY CUSTOMER\n");
        int served = 0;
        for (int i = 0; i < 1000; i++) {
            if (naive.getAccount("ana", "acc-0001").status() == 200) {
                served++;
            }
        }
        out.append("1000 requests in a loop      -> ").append(served).append(" answered, none slowed down\n");

        out.append("\n== 4. Explicit models, a page cap and a budget ==\n");
        Bank fresh = Bank.demo();
        SimClock clock = new SimClock();
        Api safe = new SafeApi(fresh, clock);
        out.append("PATCH displayName            -> ").append(summary(safe.patchProfile("ana", "acc-0001", Map.of("displayName", "Ana Ruiz")))).append('\n');
        out.append("PATCH role, creditLimit      -> ").append(summary(safe.patchProfile("ana", "acc-0001", ESCALATION))).append('\n');
        out.append("role afterwards              -> ").append(fresh.find("acc-0001").orElseThrow().get("role")).append('\n');
        out.append("GET /accounts/acc-0001       -> ").append(summary(safe.getAccount("ana", "acc-0001"))).append('\n');
        out.append("GET /accounts?size=1000000   -> ").append(summary(safe.listAccounts("ana", 0, 1_000_000))).append('\n');

        out.append("\n== 5. The budget runs out, and refills ==\n");
        int ok = 0;
        ApiResponse last = null;
        for (int i = 0; i < 1000; i++) {
            last = safe.getAccount("ana", "acc-0001");
            if (last.status() == 200) {
                ok++;
            }
        }
        out.append("1000 requests in a loop      -> ").append(ok).append(" answered, then ").append(last.status()).append(" with Retry-After ")
                .append(last.retryAfterSeconds()).append("s\n");
        clock.advance(last.retryAfterSeconds());
        out.append("after waiting that long      -> ").append(summary(safe.getAccount("ana", "acc-0001"))).append('\n');
        out.append("another customer meanwhile   -> ").append(summary(safe.listAccounts("user2", 0, 5))).append('\n');
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    static String summary(ApiResponse r) {
        Object b = r.body();
        String shown;
        if (b instanceof Map<?, ?> m) {
            shown = "fields " + m.keySet() + (m.containsKey("role") ? ", role=" + m.get("role") + ", creditLimitCents=" + m.get("creditLimitCents") : "");
        } else if (b instanceof AccountView v) {
            shown = "fields [id, displayName, email, balanceCents, cardLast4], card ****" + v.cardLast4();
        } else if (b instanceof List<?> l) {
            shown = l.size() + " accounts" + (l.isEmpty() || l.get(0) instanceof Map ? ", every field" : ", id and name only");
        } else {
            shown = String.valueOf(b);
        }
        return r.status() + " " + shown;
    }
}
