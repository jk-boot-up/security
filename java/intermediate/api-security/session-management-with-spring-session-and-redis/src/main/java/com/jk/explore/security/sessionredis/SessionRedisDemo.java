package com.jk.explore.security.sessionredis;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Runs the bank story in five numbered acts against real Spring Boot servers and a real Redis in Docker, all on the loopback interface. */
public class SessionRedisDemo {

    private static final String SESSIONS = "spring:session:sessions:";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. Only statuses, booleans and counts are printed; ids and ports never are. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        try (RedisLab redis = RedisLab.start()) {
            act1Stored(out, redis);
            act2Fixation(out, redis);
            act3Timeouts(out, redis);
            act4Ending(out, redis);
            act5Bill(out, redis);
        }
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String answer(Browser.Reply r) {
        return r.status() == 200 ? "allowed" : "refused (" + r.status() + ")";
    }

    /**
     * The live session records in Redis: a record counts while its expiry marker exists. Spring Session leaves the data of an ended session in Redis for
     * five more minutes, with no marker, and the bank no longer opens it.
     */
    static Set<String> records(RedisLab redis) {
        Set<String> keys = redis.cli(true, "keys", SESSIONS + "*").lines().collect(Collectors.toCollection(java.util.TreeSet::new));
        return keys.stream().filter(k -> !k.startsWith(SESSIONS + "expires:") && keys.contains(SESSIONS + "expires:" + k.substring(SESSIONS.length())))
                .collect(Collectors.toCollection(java.util.TreeSet::new));
    }

    /** What is wrong with the cookie the bank set, in the terms of the plain project's checker. */
    static List<String> cookieProblems(String setCookie) {
        List<String> problems = new ArrayList<>();
        if (!setCookie.startsWith("__Host-")) {
            problems.add("no __Host- prefix");
        }
        if (!setCookie.contains("Secure")) {
            problems.add("no Secure");
        }
        if (!setCookie.contains("HttpOnly")) {
            problems.add("no HttpOnly");
        }
        if (!setCookie.contains("SameSite=Strict")) {
            problems.add("no SameSite=Strict");
        }
        if (!setCookie.contains("Path=/") || setCookie.contains("Domain=")) {
            problems.add("path or domain");
        }
        return problems;
    }

    private static void act1Stored(StringBuilder out, RedisLab redis) throws IOException, InterruptedException {
        line(out, "ONE. The session lives in Redis, and Redis is now the thing to protect.");
        line(out, "  a client with no password is refused by Redis: " + redis.cli(false, "ping").startsWith("NOAUTH"));
        try (Running bank = Running.start(redis)) {
            Browser ana = bank.browser();
            Browser.Reply in = ana.signIn("ana");
            line(out, "  ana signs in: " + in.status() + ", cookie __Host-session with Path=/, Secure, HttpOnly, SameSite=Strict: problems " + cookieProblems(in.setCookies().get(0)).size());
            line(out, "  the account page with that cookie: " + ana.get("/account").status());
            Set<String> records = records(redis);
            line(out, "  session records in Redis: " + records.size() + ", an index entry for ana: "
                    + redis.cli(true, "keys", "spring:session:index:*").contains("PRINCIPAL_NAME_INDEX_NAME:ana"));
            String id = records.iterator().next().substring(SESSIONS.length());
            line(out, "  the cookie is the Redis key's id, encoded: " + Base64.getEncoder().encodeToString(id.getBytes()).equals(ana.cookie()));
            int opened = 0;
            for (String key : records) {
                Browser thief = bank.browser();
                thief.setCookie(Base64.getEncoder().encodeToString(key.substring(SESSIONS.length()).getBytes()));
                if (thief.get("/account").status() == 200) {
                    opened++;
                }
            }
            line(out, "  a stolen copy of Redis's " + records.size() + " session keys, turned into cookies, opens " + opened + " sessions  <- THE ID IS STORED AS ITSELF");
            line(out, "  the plain project kept only a hash of each id; here the protection is Redis's password, its loopback address, and short timeouts");
        }
        redis.cli(true, "flushall");
    }

    private static void act2Fixation(StringBuilder out, RedisLab redis) throws IOException, InterruptedException {
        line(out, "TWO. A new id at every sign-in.");
        for (String mode : List.of("none", "change")) {
            try (Running bank = Running.start(redis, "bank.fixation=" + mode)) {
                Browser ana = bank.browser();
                ana.get("/welcome");
                String planted = ana.cookie();
                Browser stranger = bank.browser();
                stranger.setCookie(planted);
                ana.signIn("ana");
                boolean same = planted.equals(ana.cookie());
                if (mode.equals("none")) {
                    line(out, "  a stranger gets an id from the bank's welcome page and puts it into ana's browser; a bank that keeps the id at sign-in: ana's id after signing in is the planted one: " + same);
                    line(out, "  the stranger's copy now reads ana's account: " + answer(stranger.get("/account")) + "  <- SESSION FIXATION");
                } else {
                    line(out, "  the same plan against Spring Security's default (changeSessionId): the ids differ: " + !same);
                    line(out, "  the stranger's copy: " + answer(stranger.get("/account")) + ", ana's new id: " + answer(ana.get("/account")));
                    line(out, "  the planted id is no longer a live record in Redis: " + !records(redis).contains(SESSIONS + new String(Base64.getDecoder().decode(planted))));
                }
            }
            redis.cli(true, "flushall");
        }
    }

    private static void act3Timeouts(StringBuilder out, RedisLab redis) throws IOException, InterruptedException {
        line(out, "THREE. How long a session lives.");
        line(out, "  idle timeout 2 seconds for the demo, absolute timeout 8 hours");
        try (Running bank = Running.start(redis)) {
            Browser ana = bank.browser();
            ana.signIn("ana");
            String key = records(redis).iterator().next();
            long ttl = Long.parseLong(redis.cli(true, "ttl", key));
            line(out, "  Redis keeps the record for the idle timeout plus five minutes: " + (ttl > 2 && ttl <= 302));
            Thread.sleep(1000);
            line(out, "  ana is idle for 1 second: " + answer(ana.get("/account")));
            Thread.sleep(2500);
            line(out, "  then idle for 2.5 seconds: " + answer(ana.get("/account")));
            line(out, "  Spring Session leaves the dead record in Redis for five more minutes, and the bank still refuses its id: " + redis.cli(true, "exists", key).equals("1")
                    + " (live records " + records(redis).size() + ")");
        }
        redis.cli(true, "flushall");
        try (Running bank = Running.start(redis)) {
            Browser ana = bank.browser();
            ana.signIn("ana");
            OffsetClock clock = bank.bean(OffsetClock.class);
            clock.advance(Duration.ofHours(7).plusMinutes(59));
            line(out, "  ana stays busy, and 7 hours 59 minutes after sign-in: " + answer(ana.get("/account")));
            clock.advance(Duration.ofMinutes(2));
            line(out, "  two minutes later, at 8 hours 1 minute: " + answer(ana.get("/account")) + "  (Spring Session has no absolute timeout; this is a 20-line filter of our own)");
        }
        redis.cli(true, "flushall");
    }

    private static void act4Ending(StringBuilder out, RedisLab redis) throws IOException, InterruptedException {
        line(out, "FOUR. Signing out, everywhere, and the limit.");
        try (Running bank = Running.start(redis, "spring.session.timeout=5m")) {
            Browser ana = bank.browser();
            ana.signIn("ana");
            Browser copy = bank.browser();
            copy.setCookie(ana.cookie());
            Browser.Reply logout = ana.post("/logout", "");
            line(out, "  sign-out: " + logout.status() + ", the browser is told to forget the cookie: " + logout.setCookies().stream().anyMatch(c -> c.contains("Max-Age=0"))
                    + ", the record is no longer live in Redis: " + records(redis).isEmpty());
            line(out, "  a copy of the cookie made before sign-out, sent again: " + answer(copy.get("/account")) + "  <- THE SERVER ENDED THE SESSION, NOT ONLY THE BROWSER");

            Browser phone = bank.browser();
            Browser laptop = bank.browser();
            Browser tablet = bank.browser();
            for (Browser device : List.of(phone, laptop, tablet)) {
                device.signIn("ana");
            }
            line(out, "  ana is signed in on her phone, laptop and tablet: " + records(redis).size() + " live records, " + bank.bean(org.springframework.session.FindByIndexNameSessionRepository.class).findByPrincipalName("ana").size() + " in her index");
            Browser.Reply everywhere = laptop.post("/sign-out-everywhere", "");
            line(out, "  sign out everywhere: " + everywhere.text() + ", then the phone: " + answer(phone.get("/account")) + ", the tablet: " + answer(tablet.get("/account")));

            for (Browser device : List.of(phone, laptop, tablet)) {
                device.signIn("ana");
            }
            String oldLaptop = laptop.cookie();
            Browser.Reply change = laptop.post("/password", "");
            Browser staleLaptop = bank.browser();
            staleLaptop.setCookie(oldLaptop);
            line(out, "  ana changes her password on the laptop: " + change.text() + ", the laptop goes on with a new id: " + answer(laptop.get("/account"))
                    + ", the old laptop id: " + answer(staleLaptop.get("/account")) + ", the phone: " + answer(phone.get("/account")));
        }
        redis.cli(true, "flushall");
        try (Running bank = Running.start(redis, "spring.session.timeout=5m", "bank.max-sessions=3")) {
            Browser phone = bank.browser();
            Browser laptop = bank.browser();
            Browser tablet = bank.browser();
            Browser watch = bank.browser();
            for (Browser device : List.of(phone, laptop, tablet)) {
                device.signIn("ana");
                Thread.sleep(50);
            }
            Browser.Reply fourth = watch.signIn("ana");
            line(out, "  a limit of 3 sessions: a fourth sign-in " + fourth.status() + " ends the oldest, the phone's: " + answer(phone.get("/account"))
                    + ", the laptop: " + answer(laptop.get("/account")) + ", the watch: " + answer(watch.get("/account")));
        }
        redis.cli(true, "flushall");
    }

    private static void act5Bill(StringBuilder out, RedisLab redis) throws IOException, InterruptedException {
        line(out, "FIVE. The bill.");
        try (Running a = Running.start(redis, "spring.session.timeout=5m", "spring.session.data.redis.namespace=spring:session:a");
                Running b = Running.start(redis, "spring.session.timeout=5m", "spring.session.data.redis.namespace=spring:session:b")) {
            Browser onA = a.browser();
            onA.signIn("ana");
            line(out, "  two servers with separate session stores: ana signs in on A, her next request lands on B: " + answer(onA.at(b.url()).get("/account")) + "  <- NEEDS A SHARED STORE");
        }
        redis.cli(true, "flushall");
        try (Running a = Running.start(redis, "spring.session.timeout=5m"); Running b = Running.start(redis, "spring.session.timeout=5m")) {
            Browser onA = a.browser();
            onA.signIn("ana");
            Browser onB = onA.at(b.url());
            line(out, "  both servers using the same Redis: her next request lands on B: " + answer(onB.get("/account")) + ", and back on A: " + answer(onB.at(a.url()).get("/account")));
            redis.pause();
            Browser.Reply whileDown = onB.get("/account");
            Browser.Reply signIn = b.browser().signIn("ana");
            redis.unpause();
            line(out, "  Redis stops answering: a request with a valid cookie " + answer(whileDown) + ", a sign-in " + (signIn.status() == 200 ? "allowed" : "refused (" + signIn.status() + ")") + "  <- FAILS CLOSED");
            line(out, "  Redis answers again: the same cookie " + answer(onB.get("/account")));
        }
        redis.cli(true, "flushall");
        for (boolean save : new boolean[] {true, false}) {
            try (Running bank = Running.start(redis, "bank.save-requests=" + save)) {
                for (int i = 0; i < 20; i++) {
                    bank.browser().get("/account");
                }
                line(out, "  20 requests from strangers with no cookie, with Spring Security's request cache " + (save ? "on (the default)" : "off") + ": " + records(redis).size() + " session records in Redis"
                        + (save ? "  <- A STRANGER CAN FILL REDIS" : ""));
            }
            redis.cli(true, "flushall");
        }
        line(out, "  a short idle timeout asks a customer who stops to read a statement to sign in again, and Redis is part of every request");
        line(out, "  the rules: a new id at sign-in, an idle and an absolute limit, the server ends the session, a password for Redis, and a bank that refuses when Redis is down");
    }
}
