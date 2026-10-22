package com.jk.explore.security.apispring;

import java.io.IOException;
import java.time.Duration;

/** Runs the bank API story in five numbered sections against real Spring Boot servers and a real Redis in Docker, all on the loopback interface. */
public class ApiSpringDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Only statuses, field names and counts are printed. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (RedisLab redis = RedisLab.start()) {
            try (Running api = Running.start("naive", null)) {
                section1FirstVersion(out, api);
            }
            try (Running api = Running.start("safe", redis)) {
                section2Request(out, api);
                section3Response(out, api);
            }
            redis.cli(true, "flushall");
            section4Budget(out, redis);
            redis.cli(true, "flushall");
            section5Bill(out, redis);
        }
        return out.toString();
    }

    private static void row(StringBuilder out, String label, String result) {
        out.append("%-28s -> %s%n".formatted(label, result));
    }

    private static String describe(Client.Reply r) {
        return r.status() + " fields " + r.fields();
    }

    private static void section1FirstVersion(StringBuilder out, Running api) throws IOException, InterruptedException {
        Client c = api.client();
        out.append("== 1. The first version: bind the body, return the record, let the caller choose ==\n");
        Client.Reply ok = c.patch("/accounts/me", "ana", "{\"displayName\":\"Ana K\"}");
        row(out, "PATCH displayName", describe(ok) + ", role=" + ok.json().get("role") + ", creditLimitCents=" + ok.json().get("creditLimitCents"));
        Client.Reply bad = c.patch("/accounts/me", "ana", "{\"role\":\"admin\",\"creditLimitCents\":100000000}");
        row(out, "PATCH role, creditLimit", describe(bad) + ", role=" + bad.json().get("role") + ", creditLimitCents=" + bad.json().get("creditLimitCents") + "  <- NOT HERS TO CHANGE");
        Client.Reply one = c.get("/accounts/acc-0001", "ana");
        row(out, "GET /accounts/acc-0001", describe(one) + ", role=" + one.json().get("role") + ", creditLimitCents=" + one.json().get("creditLimitCents"));
        Client.Reply list = c.get("/accounts?size=1000000", "ana");
        row(out, "GET /accounts?size=1000000", list.status() + " " + list.list().size() + " accounts, every field  <- EVERY CUSTOMER");
        int answered = 0;
        for (int i = 0; i < 1000; i++) {
            if (c.get("/accounts/acc-0002", "ana").status() == 200) {
                answered++;
            }
        }
        row(out, "1000 requests in a loop", answered + " answered, none slowed down");
    }

    private static void section2Request(StringBuilder out, Running api) throws IOException, InterruptedException {
        Client c = api.client();
        out.append("\n== 2. A request model: the fields it names, with their limits ==\n");
        Client.Reply ok = c.patch("/accounts/me", "ana", "{\"displayName\":\"Ana K\"}");
        row(out, "PATCH displayName", describe(ok) + ", card " + ok.json().get("cardLast4"));
        Client.Reply bad = c.patch("/accounts/me", "ana", "{\"role\":\"admin\",\"creditLimitCents\":100000000}");
        row(out, "PATCH role, creditLimit", bad.status() + " " + bad.detail());
        row(out, "role afterwards", api.bean(Bank.class).ownedBy("ana").getRole() + ", credit limit " + api.bean(Bank.class).ownedBy("ana").getCreditLimitCents());
        row(out, "PATCH email not an email", problem(c.patch("/accounts/me", "ana", "{\"email\":\"not-an-email\"}")));
        row(out, "PATCH name of 41 letters", problem(c.patch("/accounts/me", "ana", "{\"displayName\":\"" + "x".repeat(41) + "\"}")));
        row(out, "PATCH name left empty", problem(c.patch("/accounts/me", "ana", "{\"displayName\":\"\"}")));
        row(out, "PATCH body is not JSON", problem(c.patch("/accounts/me", "ana", "this is not json")));
    }

    private static String problem(Client.Reply r) {
        return r.status() + " " + r.detail();
    }

    private static void section3Response(StringBuilder out, Running api) throws IOException, InterruptedException {
        Client c = api.client();
        out.append("\n== 3. A response model and a page that has a ceiling ==\n");
        Client.Reply one = c.get("/accounts/acc-0001", "ana");
        row(out, "GET /accounts/acc-0001", describe(one) + ", card " + one.json().get("cardLast4"));
        Client.Reply list = c.get("/accounts?size=1000000", "ana");
        row(out, "GET /accounts?size=1000000", list.status() + " " + list.list().size() + " accounts, " + list.list().get(0).keySet() + " only");
        Client.Reply zero = c.get("/accounts?size=0", "ana");
        row(out, "GET /accounts?size=0", zero.status() + " " + zero.detail());
        row(out, "GET /accounts, no login", String.valueOf(c.get("/accounts", null).status()));
    }

    private static void section4Budget(StringBuilder out, RedisLab redis) throws IOException, InterruptedException {
        out.append("\n== 4. The budget runs out, and refills ==\n");
        try (Running api = Running.start("safe", redis)) {
            Client c = api.client();
            int answered = 0;
            Client.Reply last = null;
            for (int i = 0; i < 1000; i++) {
                last = c.get("/accounts/acc-0001", "ana");
                if (last.status() == 200) {
                    answered++;
                }
            }
            row(out, "1000 requests in a loop", answered + " answered, then " + last.status() + " with Retry-After " + last.header("retry-after") + "s");
            api.bean(ManualClock.class).advance(Duration.ofSeconds(1));
            Client.Reply after = c.get("/accounts/acc-0001", "ana");
            row(out, "after waiting that long", describe(after) + ", card " + after.json().get("cardLast4"));
            Client.Reply ben = c.get("/accounts?size=5", "ben");
            row(out, "another customer meanwhile", ben.status() + " " + ben.list().size() + " accounts, " + ben.list().get(0).keySet() + " only");
            row(out, "the bucket in Redis", "rate:ana and rate:ben: " + redis.cli(true, "keys", "rate:*").lines().sorted().toList());
        }
    }

    private static void section5Bill(StringBuilder out, RedisLab redis) throws IOException, InterruptedException {
        out.append("\n== 5. The bill: one budget for every server, and a counter that must answer ==\n");
        for (String limiter : new String[] {"local", "redis"}) {
            try (Running a = Running.start("safe", redis, "bank.limiter=" + limiter); Running b = Running.start("safe", redis, "bank.limiter=" + limiter)) {
                Client onA = a.client();
                Client onB = onA.at(b.url());
                int answered = 0;
                for (int i = 0; i < 1000; i++) {
                    if ((i % 2 == 0 ? onA : onB).get("/accounts/acc-0001", "ana").status() == 200) {
                        answered++;
                    }
                }
                row(out, "two servers, " + limiter + " budget", answered + " of 1000 requests answered" + (limiter.equals("local") ? "  <- A BUDGET PER SERVER, NOT PER CALLER" : ""));
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
            row(out, "Redis stops answering", down.status() + ", the API refuses rather than run without a budget");
            row(out, "Redis answers again", String.valueOf(c.get("/accounts/acc-0001", "ana").status()));
        }
        out.append("a budget protects the service and every other caller, not the data: it does not replace the request model or the response model\n");
        out.append("the rules: name the fields in and out, give lists a ceiling, validate the values, count requests per caller in one place that every server shares\n");
    }
}
