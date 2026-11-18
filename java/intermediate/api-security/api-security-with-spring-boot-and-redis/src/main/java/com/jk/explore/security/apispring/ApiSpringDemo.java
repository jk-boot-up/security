package com.jk.explore.security.apispring;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs the bank API story in five numbered acts against real Spring Boot servers and a real Redis in Docker, all on the loopback interface.
 * Each call is printed as THE REQUEST and THE RESPONSE in separate sections, and the numbers behind the budget are worked out with the real values.
 */
public class ApiSpringDemo {

    static final String ESCALATION = "{\"role\":\"admin\",\"creditLimitCents\":100000000}";
    static final String RENAME = "{\"displayName\":\"Ana K\"}";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Only fixed data, statuses, field names and counts are printed. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (RedisLab redis = RedisLab.start()) {
            try (Running api = Running.start("naive", null)) {
                act1FirstVersion(out, api);
            }
            try (Running api = Running.start("safe", redis)) {
                act2Request(out, api);
                act3Response(out, api);
            }
            redis.cli(true, "flushall");
            act4Budget(out, redis);
            redis.cli(true, "flushall");
            act5Bill(out, redis);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** One field of a record: the name in a column, the value, and an optional mark. */
    private static void field(StringBuilder out, Object name, Object value, String mark) {
        line(out, String.format("    %-18s%s%s", name, value, mark.isEmpty() ? "" : "  <- " + mark));
    }

    private static void request(StringBuilder out, String header, String call, String body) {
        line(out, "  " + header);
        line(out, "    " + call);
        if (body != null) {
            line(out, "    " + body);
        }
    }

    private static void act1FirstVersion(StringBuilder out, Running api) throws IOException, InterruptedException {
        Client c = api.client();
        line(out, "ONE. The first version binds the body and returns the record.");
        request(out, "THE REQUEST (Ana's app, signed in as ana):", "PATCH /accounts/me", RENAME);
        Client.Reply ok = c.patch("/accounts/me", "ana", RENAME);
        Map<String, Object> record = ok.json();
        line(out, "  THE RESPONSE (" + ok.status() + ", the stored record: " + record.size() + " fields):");
        record.forEach((k, v) -> field(out, k, v, ""));
        request(out, "THE REQUEST (two keys added by hand):", "PATCH /accounts/me", ESCALATION);
        Client.Reply bad = c.patch("/accounts/me", "ana", ESCALATION);
        line(out, "  THE RESPONSE (" + bad.status() + ", every key bound onto the record):");
        field(out, "role", bad.json().get("role"), "NOT HERS TO SET");
        field(out, "creditLimitCents", bad.json().get("creditLimitCents"), "NOT HERS TO SET");
        line(out, "  THE REQUEST:");
        line(out, "    GET /accounts?size=1000000");
        List<Map<String, Object>> all = c.get("/accounts?size=1000000", "ana").list();
        int perRow = all.get(0).size();
        line(out, "  THE RESPONSE (200):");
        line(out, "    " + all.size() + " accounts x " + perRow + " fields = " + all.size() * perRow + " values  <- EVERY CUSTOMER");
        line(out, "  THE REQUESTS:");
        line(out, "    1000 x GET /accounts/acc-0001, in a loop");
        int answered = 0;
        for (int i = 0; i < 1000; i++) {
            if (c.get("/accounts/acc-0001", "ana").status() == 200) {
                answered++;
            }
        }
        line(out, "  THE RESPONSES:");
        line(out, "    " + answered + " answered with 200, none slowed down");
    }

    /** The limits of each field of the request model, read from its annotations. */
    static List<String> modelLimits() {
        List<String> limits = new ArrayList<>();
        for (RecordComponent rc : UpdateProfileRequest.class.getRecordComponents()) {
            List<String> parts = new ArrayList<>();
            if (rc.getAccessor().isAnnotationPresent(Email.class)) {
                parts.add("an e-mail address");
            }
            Size size = rc.getAccessor().getAnnotation(Size.class);
            if (size != null) {
                parts.add(size.min() > 0 ? size.min() + " to " + size.max() + " characters" : "at most " + size.max() + " characters");
            }
            limits.add(String.format("%-18s%s", rc.getName(), String.join(", ", parts)));
        }
        return limits;
    }

    private static void act2Request(StringBuilder out, Running api) throws IOException, InterruptedException {
        Client c = api.client();
        line(out, "TWO. A request model names the fields, with their limits.");
        line(out, "  THE MODEL (UpdateProfileRequest):");
        modelLimits().forEach(l -> line(out, "    " + l));
        line(out, "    any other field   refused: fail-on-unknown-properties="
                + api.context().getEnvironment().getProperty("spring.jackson.deserialization.fail-on-unknown-properties"));
        Bank bank = api.bean(Bank.class);
        line(out, "  THE RECORD BEFORE:");
        field(out, "role", bank.ownedBy("ana").getRole(), "");
        field(out, "creditLimitCents", bank.ownedBy("ana").getCreditLimitCents(), "");
        line(out, "  THE REQUESTS AND RESPONSES (PATCH /accounts/me):");
        Client.Reply ok = c.patch("/accounts/me", "ana", RENAME);
        line(out, "    " + RENAME);
        line(out, "      -> " + ok.status() + ", an AccountView");
        String[][] refused = {
                {ESCALATION, ESCALATION},
                {"{\"email\":\"not-an-email\"}", "{\"email\":\"not-an-email\"}"},
                {"{\"displayName\":\"" + "x".repeat(41) + "\"}", "{\"displayName\":\"xxx...x\"}  (41 letters)"},
                {"{\"displayName\":\"\"}", "{\"displayName\":\"\"}"},
                {"this is not json", "this is not json"},
        };
        for (String[] r : refused) {
            Client.Reply reply = c.patch("/accounts/me", "ana", r[0]);
            line(out, "    " + r[1]);
            line(out, "      -> " + reply.status() + " " + reply.detail());
        }
        line(out, "  THE RECORD AFTER (nothing but the name changed):");
        field(out, "role", bank.ownedBy("ana").getRole(), "");
        field(out, "creditLimitCents", bank.ownedBy("ana").getCreditLimitCents(), "");
    }

    private static void act3Response(StringBuilder out, Running api) throws IOException, InterruptedException {
        Client c = api.client();
        line(out, "THREE. A response model, and a page with a ceiling.");
        line(out, "  THE REQUEST:");
        line(out, "    GET /accounts/acc-0001");
        Client.Reply one = c.get("/accounts/acc-0001", "ana");
        Map<String, Object> view = one.json();
        line(out, "  THE RESPONSE (" + one.status() + ", an AccountView: " + view.size() + " fields):");
        view.forEach((k, v) -> field(out, k, v, ""));
        line(out, "    no role, no limit, no risk score, no PIN hash");
        line(out, "  THE REQUEST:");
        line(out, "    GET /accounts?size=1000000");
        int max = Integer.parseInt(api.context().getEnvironment().getProperty("bank.page.max"));
        line(out, "  THE PAGE SIZE (bank.page.max=" + max + "):");
        line(out, "    min(1000000, " + max + ") = " + Math.min(1_000_000, max));
        Client.Reply list = c.get("/accounts?size=1000000", "ana");
        line(out, "  THE RESPONSE (" + list.status() + ", a DirectoryEntry per account):");
        line(out, "    " + list.list().size() + " accounts x " + list.list().get(0).size() + " fields " + list.list().get(0).keySet());
        line(out, "  THE REQUEST:");
        line(out, "    GET /accounts?size=0");
        Client.Reply zero = c.get("/accounts?size=0", "ana");
        line(out, "  THE RESPONSE:");
        line(out, "    " + zero.status() + " " + zero.detail() + "  (size has @Min(1))");
        line(out, "  THE REQUEST:");
        line(out, "    GET /accounts, with no login");
        line(out, "  THE RESPONSE:");
        line(out, "    " + c.get("/accounts", null).status() + ", before any controller runs");
    }

    private static void act4Budget(StringBuilder out, RedisLab redis) throws IOException, InterruptedException {
        line(out, "FOUR. The budget runs out, and refills.");
        try (Running api = Running.start("safe", redis)) {
            Client c = api.client();
            int capacity = Integer.parseInt(api.context().getEnvironment().getProperty("bank.budget.capacity"));
            double perSecond = Double.parseDouble(api.context().getEnvironment().getProperty("bank.budget.refill-per-second"));
            line(out, "  THE BUDGET (one bucket per caller, kept in Redis):");
            line(out, "    holds " + capacity + " tokens, refills " + (long) perSecond + " a second; a request costs 1");
            line(out, "  THE REQUESTS:");
            line(out, "    1000 x GET /accounts/acc-0001 as ana, at one moment");
            int answered = 0;
            Client.Reply last = null;
            for (int i = 0; i < 1000; i++) {
                last = c.get("/accounts/acc-0001", "ana");
                if (last.status() == 200) {
                    answered++;
                }
            }
            line(out, "  THE RESPONSES:");
            line(out, "    " + answered + " answered with 200");
            line(out, "    " + (1000 - answered) + " refused with " + last.status() + ", Retry-After: " + last.header("retry-after"));
            double tokens = Double.parseDouble(redis.cli(true, "hget", "rate:ana", "tokens"));
            double perMilli = perSecond / 1000.0;
            long millis = RateLimiter.retryAfterMillis(tokens, perMilli);
            line(out, "  HOW Retry-After IS WORKED OUT:");
            line(out, "    tokens left " + whole(tokens) + ", refill " + perMilli + " a millisecond");
            line(out, "    wait = ceil((1 - " + whole(tokens) + ") / " + perMilli + ") = " + millis + " ms");
            line(out, "    Retry-After = ceil(" + millis + " / 1000) = " + RateLimiter.retryAfterSeconds(millis) + " second");
            api.bean(ManualClock.class).advance(Duration.ofSeconds(1));
            line(out, "  THE REQUEST, 1 SECOND LATER:");
            line(out, "    GET /accounts/acc-0001");
            Client.Reply after = c.get("/accounts/acc-0001", "ana");
            line(out, "  THE RESPONSE:");
            line(out, "    " + after.status() + "; tokens " + whole(tokens) + " + " + (long) perSecond + " refilled - 1 taken = "
                    + whole(Double.parseDouble(redis.cli(true, "hget", "rate:ana", "tokens"))));
            Client.Reply ben = c.get("/accounts?size=5", "ben");
            line(out, "  ANOTHER CUSTOMER MEANWHILE (ben, own bucket):");
            line(out, "    GET /accounts?size=5 -> " + ben.status() + ", " + ben.list().size() + " accounts");
            line(out, "  THE KEYS IN REDIS:");
            line(out, "    " + redis.cli(true, "keys", "rate:*").lines().sorted().toList());
        }
    }

    private static String whole(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    private static void act5Bill(StringBuilder out, RedisLab redis) throws IOException, InterruptedException {
        line(out, "FIVE. The bill: one shared budget, and a counter that must answer.");
        for (String limiter : new String[] {"local", "redis"}) {
            try (Running a = Running.start("safe", redis, "bank.limiter=" + limiter); Running b = Running.start("safe", redis, "bank.limiter=" + limiter)) {
                Client onA = a.client();
                Client onB = onA.at(b.url());
                int onServerA = 0;
                int onServerB = 0;
                for (int i = 0; i < 1000; i++) {
                    boolean toA = i % 2 == 0;
                    if ((toA ? onA : onB).get("/accounts/acc-0001", "ana").status() == 200) {
                        if (toA) {
                            onServerA++;
                        } else {
                            onServerB++;
                        }
                    }
                }
                line(out, limiter.equals("local") ? "  TWO SERVERS, A BUDGET IN EACH ONE'S MEMORY:" : "  TWO SERVERS, ONE BUDGET IN REDIS:");
                line(out, "    1000 requests as ana, every other one to server B");
                line(out, "    -> " + onServerA + " + " + onServerB + " = " + (onServerA + onServerB) + " answered"
                        + (limiter.equals("local") ? "  <- TWICE THE BUDGET" : ""));
            }
            redis.cli(true, "flushall");
        }
        try (Running api = Running.start("safe", redis)) {
            Client c = api.client();
            c.get("/accounts/acc-0001", "ana");
            redis.pause();
            Client.Reply down;
            try {
                down = c.get("/accounts/acc-0001", "ana");
            } finally {
                redis.unpause();
            }
            line(out, "  REDIS STOPS ANSWERING (the container is paused):");
            line(out, "    GET /accounts/acc-0001 -> " + down.status() + ", no budget, no answer");
            line(out, "  REDIS ANSWERS AGAIN:");
            line(out, "    GET /accounts/acc-0001 -> " + c.get("/accounts/acc-0001", "ana").status());
        }
        line(out, "  a budget protects the service, not the data:");
        line(out, "  it does not replace the request or the response model");
    }
}
