package com.jk.explore.security.apisec;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Runs the bank API story in five numbered acts and prints exact, repeatable results. Each call is shown as THE REQUEST and
 * THE RESPONSE in separate sections, and the numbers behind a page cap and a rate limit are worked out with the real values.
 */
public class ApiSecurityDemo {

    /** Ana's own request, with two keys added by hand. Ordered, so the printed JSON is the same on every run. */
    static final Map<String, Object> ESCALATION = ordered("displayName", "Ana", "role", "admin", "creditLimitCents", 100_000_000L);
    static final Map<String, Object> RENAME = ordered("displayName", "Ana Ruiz");
    static final String ANA = "acc-0001";

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        Bank bank = Bank.demo();
        Api naive = new NaiveApi(bank);
        Account ana = bank.find(ANA).orElseThrow();

        line(out, "ONE. A profile call that works.");
        request(out, "THE REQUEST (Ana's app changes her display name):", "PATCH /accounts/" + ANA, RENAME);
        Map<?, ?> full = (Map<?, ?>) naive.patchProfile("ana", ANA, RENAME).body();
        line(out, "  THE RESPONSE (200, the account record itself):");
        full.forEach((k, v) -> field(out, k, v, ""));
        line(out, "  the app shows one field; the response carries " + full.size());

        line(out, "TWO. Whatever the caller sends becomes a field.");
        request(out, "THE REQUEST (two keys added by hand):", "PATCH /accounts/" + ANA, ESCALATION);
        line(out, "  THE RECORD BEFORE:");
        field(out, "role", ana.get("role"), "");
        field(out, "creditLimitCents", ana.get("creditLimitCents"), "");
        ApiResponse escalated = naive.patchProfile("ana", ANA, ESCALATION);
        line(out, "  THE RESPONSE (" + escalated.status() + ", every key copied onto the record):");
        Map<?, ?> after = (Map<?, ?>) escalated.body();
        field(out, "role", after.get("role"), "NOT HERS TO SET");
        field(out, "creditLimitCents", after.get("creditLimitCents"), "NOT HERS TO SET");
        line(out, "  the API never decided which fields were hers");

        line(out, "THREE. The response is the whole record, and a list has no end.");
        line(out, "  THE REQUEST:");
        line(out, "    GET /accounts/" + ANA);
        Map<?, ?> record = (Map<?, ?>) naive.getAccount("ana", ANA).body();
        line(out, "  THE RESPONSE (200, " + record.size() + " fields, among them):");
        field(out, "cardNumber", record.get("cardNumber"), "FULL CARD");
        field(out, "pinHash", record.get("pinHash"), "PIN HASH");
        field(out, "riskScore", record.get("riskScore"), "INTERNAL");
        line(out, "  THE REQUEST:");
        line(out, "    GET /accounts?page=0&size=1000000");
        List<?> everyone = (List<?>) naive.listAccounts("ana", 0, 1_000_000).body();
        int perRow = ((Map<?, ?>) everyone.get(0)).size();
        line(out, "  THE RESPONSE (200):");
        line(out, "    " + everyone.size() + " accounts x " + perRow + " fields = " + everyone.size() * perRow + " values"
                + "  <- EVERY CUSTOMER");
        line(out, "  THE REQUESTS:");
        line(out, "    1000 x GET /accounts/" + ANA + ", in a loop");
        int served = 0;
        for (int i = 0; i < 1000; i++) {
            if (naive.getAccount("ana", ANA).status() == 200) {
                served++;
            }
        }
        line(out, "  THE RESPONSES:");
        line(out, "    " + served + " answered with 200, none slowed down");

        line(out, "FOUR. Explicit models and a page cap.");
        Bank fresh = Bank.demo();
        SimClock clock = new SimClock();
        SafeApi safe = new SafeApi(fresh, clock);
        request(out, "THE REQUEST (the same two keys added by hand):", "PATCH /accounts/" + ANA, ESCALATION);
        line(out, "  THE CHECK (UpdateProfileRequest names " + String.join(", ", UpdateProfileRequest.FIELDS) + "):");
        ESCALATION.keySet().forEach(k -> field(out, k, UpdateProfileRequest.FIELDS.contains(k) ? "named" : "not named",
                UpdateProfileRequest.FIELDS.contains(k) ? "" : "UNKNOWN"));
        ApiResponse refused = safe.patchProfile("ana", ANA, ESCALATION);
        line(out, "  THE RESPONSE:");
        line(out, "    " + refused.status() + " " + refused.body());
        line(out, "  THE RECORD AFTER (nothing changed):");
        Account freshAna = fresh.find(ANA).orElseThrow();
        field(out, "role", freshAna.get("role"), "");
        field(out, "creditLimitCents", freshAna.get("creditLimitCents"), "");
        request(out, "THE REQUEST (a field the model names):", "PATCH /accounts/" + ANA, RENAME);
        AccountView view = (AccountView) safe.patchProfile("ana", ANA, RENAME).body();
        line(out, "  THE RESPONSE (200, an AccountView with 5 fields):");
        field(out, "id", view.id(), "");
        field(out, "displayName", view.displayName(), "");
        field(out, "email", view.email(), "");
        field(out, "balanceCents", view.balanceCents(), "");
        field(out, "cardLast4", view.cardLast4() + "  (the app shows ****" + view.cardLast4() + ")", "");
        line(out, "  THE REQUEST:");
        line(out, "    GET /accounts/" + ANA);
        ApiResponse shown = safe.getAccount("ana", ANA);
        line(out, "  THE RESPONSE (" + shown.status() + "):");
        line(out, "    the same 5 fields; no card number, no PIN hash");
        line(out, "  THE REQUEST:");
        line(out, "    GET /accounts?page=0&size=1000000");
        line(out, "  THE PAGE SIZE (capped at " + SafeApi.MAX_SIZE + "):");
        line(out, "    min(1000000, " + SafeApi.MAX_SIZE + ") = " + Math.min(1_000_000, SafeApi.MAX_SIZE));
        List<?> page = (List<?>) safe.listAccounts("ana", 0, 1_000_000).body();
        line(out, "  THE RESPONSE (200, a DirectoryEntry per account):");
        line(out, "    " + page.size() + " accounts x 2 fields (id, displayName)");
        for (Object o : page.subList(0, 2)) {
            DirectoryEntry e = (DirectoryEntry) o;
            line(out, "    " + e.id() + "  " + e.displayName());
        }
        line(out, "    ... " + (page.size() - 2) + " more");

        line(out, "FIVE. The budget runs out, and refills.");
        RateLimiter reads = safe.reads();
        line(out, "  THE BUDGET (one bucket of read tokens per caller):");
        line(out, "    holds " + whole(reads.capacity()) + " tokens, refills " + whole(reads.capacity()) + " a minute = "
                + two(reads.perSecond()) + " a second");
        line(out, "    one account costs 1 token; a list costs 1 per row");
        line(out, "  ANA'S TOKENS BEFORE THE LOOP:");
        line(out, "    " + whole(reads.capacity()) + " - 1 (one account) - " + page.size() + " (a page of " + page.size() + ") = "
                + whole(reads.tokens("ana")));
        line(out, "  THE REQUESTS:");
        line(out, "    1000 x GET /accounts/" + ANA + ", in a loop");
        int ok = 0;
        ApiResponse last = null;
        for (int i = 0; i < 1000; i++) {
            last = safe.getAccount("ana", ANA);
            if (last.status() == 200) {
                ok++;
            }
        }
        line(out, "  THE RESPONSES:");
        line(out, "    " + ok + " answered with 200");
        line(out, "    " + (1000 - ok) + " refused with " + last.status() + ", Retry-After: " + last.retryAfterSeconds());
        double left = reads.tokens("ana");
        line(out, "  HOW Retry-After IS WORKED OUT:");
        line(out, "    tokens " + two(left) + ", a call costs 1, refill " + two(reads.perSecond()) + " a second");
        line(out, "    wait = ceil((1 - " + two(left) + ") / " + two(reads.perSecond()) + ") = ceil("
                + two((1 - left) / reads.perSecond()) + ") = " + last.retryAfterSeconds() + " second");
        clock.advance(last.retryAfterSeconds());
        double refilled = reads.tokens("ana");
        line(out, "  THE REQUEST, " + last.retryAfterSeconds() + " SECOND LATER:");
        line(out, "    GET /accounts/" + ANA);
        ApiResponse again = safe.getAccount("ana", ANA);
        line(out, "  THE RESPONSE:");
        line(out, "    " + again.status() + "; tokens " + two(left) + " + " + two(refilled - left) + " = " + two(refilled)
                + ", minus 1 = " + two(reads.tokens("ana")) + " left");
        ApiResponse other = safe.listAccounts("user2", 0, 5);
        line(out, "  ANOTHER CUSTOMER MEANWHILE (user2, own bucket):");
        line(out, "    GET /accounts?size=5 -> " + other.status() + ", " + ((List<?>) other.body()).size() + " accounts; "
                + whole(reads.tokens("user2")) + " tokens left");
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** One field of a record: the name in a column, the value, and an optional mark. */
    private static void field(StringBuilder out, Object name, Object value, String mark) {
        line(out, String.format("    %-18s%s%s", name, value, mark.isEmpty() ? "" : "  <- " + mark));
    }

    /** The method and path, then the JSON body one key per line. */
    private static void request(StringBuilder out, String header, String call, Map<String, Object> body) {
        line(out, "  " + header);
        line(out, "    " + call);
        int i = 0;
        for (var e : body.entrySet()) {
            Object v = e.getValue() instanceof String s ? "\"" + s + "\"" : e.getValue();
            line(out, "    " + (i == 0 ? "{" : " ") + "\"" + e.getKey() + "\": " + v + (++i == body.size() ? "}" : ","));
        }
    }

    private static String two(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }

    private static String whole(double d) {
        return String.valueOf(Math.round(d));
    }

    private static Map<String, Object> ordered(Object... keysAndValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            m.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return Collections.unmodifiableMap(m);
    }
}
