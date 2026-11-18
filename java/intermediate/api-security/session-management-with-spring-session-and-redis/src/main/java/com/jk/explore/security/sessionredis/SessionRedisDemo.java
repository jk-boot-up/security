package com.jk.explore.security.sessionredis;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.session.FindByIndexNameSessionRepository;

/**
 * Runs the bank story in five numbered acts against real Spring Boot servers and a real Redis in Docker, all on the loopback interface. Each
 * act prints what goes in and what comes out in separate, labelled sections. Ids and ports are random, so they appear only as placeholders.
 */
public class SessionRedisDemo {

    /** A fixed example id, to show how a cookie is made. Spring Session's real ids are random UUIDs. */
    static final String EXAMPLE_ID = "3f2a7c1e-9b4d-4e8a-a6f0-5c1d2e3b4a90";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
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

    static Set<String> keys(RedisLab redis) {
        return redis.cli(true, "keys", "spring:session:*").lines().filter(k -> !k.isBlank()).collect(Collectors.toCollection(TreeSet::new));
    }

    /** The live session records in Redis (see {@link SessionKeys#live}). */
    static Set<String> records(RedisLab redis) {
        return SessionKeys.live(keys(redis));
    }

    private static void act1Stored(StringBuilder out, RedisLab redis) throws IOException, InterruptedException {
        line(out, "ONE. The session lives in Redis, which must be protected.");
        line(out, "  THE REQUEST (redis-cli, no password): PING");
        line(out, "  THE ANSWER: " + redis.cli(false, "ping").split(" ")[0] + " (refused)");
        try (Running bank = Running.start(redis, "spring.session.timeout=5m")) {
            Browser ana = bank.browser();
            Browser.Reply in = ana.signIn("ana");
            line(out, "  THE SIGN-IN RESPONSE (a real Spring Boot server):");
            line(out, "    status " + in.status());
            header(out, "Set-Cookie: " + in.setCookies().get(0).replace(ana.cookie(), "<cookie>"));
            line(out, "    problems found: " + SessionKeys.cookieProblems(in.setCookies().get(0)).size());
            line(out, "  THE ACCOUNT PAGE, WITH THAT COOKIE: status " + ana.get("/account").status());
            String id = SessionKeys.idFrom(ana.cookie());
            line(out, "  WHAT REDIS HOLDS NOW (the id shown as <id>):");
            for (String key : keys(redis).stream().map(k -> SessionKeys.shown(k, id)).collect(Collectors.toCollection(TreeSet::new))) {
                line(out, "    " + key);
            }
            List<String> fields = List.of(redis.cli(true, "hkeys", SessionKeys.recordKey(id)).split("\n"));
            line(out, "  THE FIELDS OF spring:session:sessions:<id>:");
            List<String> sorted = List.copyOf(new TreeSet<>(fields));
            for (int i = 0; i < sorted.size(); i += 2) {
                line(out, "    " + String.join(", ", sorted.subList(i, Math.min(i + 2, sorted.size()))));
            }
            line(out, "  HOW THE COOKIE IS MADE (an example id; real ones are random):");
            line(out, "    the id, " + EXAMPLE_ID.length() + " characters: " + EXAMPLE_ID);
            String first = EXAMPLE_ID.substring(0, 3);
            List<String> bytes = new java.util.ArrayList<>();
            for (byte b : first.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
                bytes.add(String.format("%02x", b));
            }
            line(out, "    the first 3 characters, " + first + ", are the bytes " + String.join(" ", bytes));
            List<String> picked = new java.util.ArrayList<>();
            for (String group : SessionKeys.sixBitGroups(first)) {
                picked.add(group + "=" + SessionKeys.ALPHABET.charAt(Integer.parseInt(group, 2)));
            }
            line(out, "    6 bits a character: " + String.join(" ", picked));
            line(out, "    its Base64, " + SessionKeys.cookieFor(EXAMPLE_ID).length() + " characters:");
            line(out, "      " + SessionKeys.cookieFor(EXAMPLE_ID));
            line(out, "    ana's real cookie is the Base64 of her Redis id: " + SessionKeys.cookieFor(id).equals(ana.cookie()));
            Set<String> records = records(redis);
            int opened = 0;
            for (String key : records) {
                Browser thief = bank.browser();
                thief.setCookie(SessionKeys.cookieFor(key.substring(SessionKeys.SESSIONS.length())));
                if (thief.get("/account").status() == 200) {
                    opened++;
                }
            }
            line(out, "  THE REQUEST (a stolen copy of Redis's " + records.size() + " session keys, as cookies):");
            line(out, "  THE ANSWER: " + opened + " sessions opened  <- REDIS HOLDS LIVE IDS");
            line(out, "  the plain project kept a hash; here Redis needs a password,");
            line(out, "  the loopback address only, and short timeouts");
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
                    line(out, "  BEFORE ANA SIGNS IN:");
                    line(out, "    a stranger opens /welcome and gets the cookie <planted>");
                    line(out, "    and puts <planted> into ana's browser");
                    line(out, "  AFTER SIGN-IN, A BANK THAT KEEPS THE ID (fixation none):");
                    line(out, "    ana's cookie is still <planted>: " + same);
                    line(out, "    the stranger sends <planted>: " + answer(stranger.get("/account")) + "  <- SESSION FIXATION");
                } else {
                    line(out, "  AFTER SIGN-IN, THE DEFAULT (changeSessionId):");
                    line(out, "    ana's cookie is still <planted>: " + same + "; she now has <new>");
                    line(out, "    the stranger sends <planted>: " + answer(stranger.get("/account")));
                    line(out, "    ana sends <new>: " + answer(ana.get("/account")));
                    line(out, "    <planted> is a live record in Redis: "
                            + records(redis).contains(SessionKeys.recordKey(SessionKeys.idFrom(planted))));
                }
            }
            redis.cli(true, "flushall");
        }
    }

    private static void act3Timeouts(StringBuilder out, RedisLab redis) throws IOException, InterruptedException {
        line(out, "THREE. How long a session lives.");
        line(out, "  THE SETTINGS:");
        line(out, "    idle timeout      2 seconds (spring.session.timeout, short");
        line(out, "                      for the demo)");
        line(out, "    absolute timeout  8 hours (a filter of our own)");
        try (Running bank = Running.start(redis)) {
            Browser ana = bank.browser();
            ana.signIn("ana");
            String key = records(redis).iterator().next();
            long ttl = Long.parseLong(redis.cli(true, "ttl", key));
            line(out, "  WHAT REDIS DOES WITH THE IDLE TIMEOUT:");
            line(out, "    the record's time to live, at most 2 + 300 = 302 seconds: " + (ttl > 2 && ttl <= 302));
            line(out, "  IDLE (seconds since ana's last request -> the answer):");
            ana.get("/account");
            Thread.sleep(1000);
            line(out, "    1   -> " + answer(ana.get("/account")));
            Thread.sleep(2500);
            line(out, "    2.5 -> " + answer(ana.get("/account")));
            line(out, "    the dead record is still in Redis: " + redis.cli(true, "exists", key).equals("1")
                    + "; live records: " + records(redis).size());
        }
        redis.cli(true, "flushall");
        try (Running bank = Running.start(redis)) {
            Browser ana = bank.browser();
            ana.signIn("ana");
            OffsetClock clock = bank.bean(OffsetClock.class);
            line(out, "  ABSOLUTE (ana stays busy; the bank's clock is moved forward):");
            clock.advance(Duration.ofHours(7).plusMinutes(59));
            line(out, "    7:59 after sign-in -> " + answer(ana.get("/account")));
            clock.advance(Duration.ofMinutes(2));
            line(out, "    8:01 after sign-in -> " + answer(ana.get("/account")));
            line(out, "    created + 8:00 is before now, so the filter ends it");
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
            line(out, "  THE SIGN-OUT RESPONSE:");
            line(out, "    status " + logout.status());
            line(out, "    the browser is told to forget the cookie (Max-Age=0): "
                    + logout.setCookies().stream().anyMatch(c -> c.contains("Max-Age=0")));
            line(out, "    the record is still live in Redis: " + !records(redis).isEmpty());
            line(out, "  A COPY MADE BEFORE SIGN-OUT, SENT AGAIN:");
            line(out, "    " + answer(copy.get("/account")) + "  <- THE SERVER ENDED THE SESSION");

            Browser phone = bank.browser();
            Browser laptop = bank.browser();
            Browser tablet = bank.browser();
            for (Browser device : List.of(phone, laptop, tablet)) {
                device.signIn("ana");
            }
            int indexed = bank.bean(FindByIndexNameSessionRepository.class).findByPrincipalName("ana").size();
            line(out, "  ANA IS SIGNED IN ON HER PHONE, LAPTOP AND TABLET:");
            line(out, "    live records in Redis: " + records(redis).size() + "; in her index: " + indexed);
            Browser.Reply everywhere = laptop.post("/sign-out-everywhere", "");
            line(out, "  SIGN OUT EVERYWHERE (from the laptop):");
            line(out, "    " + everywhere.text());
            line(out, "    the phone: " + answer(phone.get("/account")) + "; the tablet: " + answer(tablet.get("/account")));

            for (Browser device : List.of(phone, laptop, tablet)) {
                device.signIn("ana");
            }
            String oldLaptop = laptop.cookie();
            Browser.Reply change = laptop.post("/password", "");
            Browser staleLaptop = bank.browser();
            staleLaptop.setCookie(oldLaptop);
            line(out, "  A PASSWORD CHANGE ON THE LAPTOP:");
            line(out, "    " + change.text());
            line(out, "    the laptop's new id: " + answer(laptop.get("/account")) + "; its old id: " + answer(staleLaptop.get("/account")));
            line(out, "    the phone: " + answer(phone.get("/account")));
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
            line(out, "  A LIMIT OF 3, THEN A FOURTH SIGN-IN ON HER WATCH:");
            line(out, "    status " + fourth.status());
            line(out, "    the phone, the oldest: " + answer(phone.get("/account")));
            line(out, "    the laptop: " + answer(laptop.get("/account")) + "; the watch: " + answer(watch.get("/account")));
        }
        redis.cli(true, "flushall");
    }

    private static void act5Bill(StringBuilder out, RedisLab redis) throws IOException, InterruptedException {
        line(out, "FIVE. The bill.");
        line(out, "  THE REQUEST: ana signs in on server A; the next lands on B");
        try (Running a = Running.start(redis, "spring.session.timeout=5m", "spring.session.data.redis.namespace=spring:session:a");
                Running b = Running.start(redis, "spring.session.timeout=5m", "spring.session.data.redis.namespace=spring:session:b")) {
            Browser onA = a.browser();
            onA.signIn("ana");
            line(out, "  THE ANSWER FROM B, EACH SERVER WITH ITS OWN STORE:");
            line(out, "    " + answer(onA.at(b.url()).get("/account")) + "  <- NEEDS A SHARED STORE");
        }
        redis.cli(true, "flushall");
        try (Running a = Running.start(redis, "spring.session.timeout=5m"); Running b = Running.start(redis, "spring.session.timeout=5m")) {
            Browser onA = a.browser();
            onA.signIn("ana");
            Browser onB = onA.at(b.url());
            line(out, "  THE ANSWER FROM B, BOTH USING THE SAME REDIS:");
            line(out, "    " + answer(onB.get("/account")) + "; and back on A: " + answer(onB.at(a.url()).get("/account")));
            redis.pause();
            Browser.Reply whileDown = onB.get("/account");
            Browser.Reply signIn = b.browser().signIn("ana");
            redis.unpause();
            line(out, "  WHILE REDIS STOPS ANSWERING:");
            line(out, "    a request with a valid cookie: " + answer(whileDown));
            line(out, "    a sign-in: " + answer(signIn) + "  <- FAILS CLOSED");
            line(out, "  AFTER REDIS ANSWERS AGAIN:");
            line(out, "    the same cookie: " + answer(onB.get("/account")));
        }
        redis.cli(true, "flushall");
        line(out, "  20 REQUESTS FROM STRANGERS WITH NO COOKIE (records in Redis):");
        for (boolean save : new boolean[] {true, false}) {
            try (Running bank = Running.start(redis, "spring.session.timeout=5m", "bank.save-requests=" + save)) {
                for (int i = 0; i < 20; i++) {
                    bank.browser().get("/account");
                }
                line(out, "    request cache " + (save ? "on, the default: " : "off: ") + records(redis).size()
                        + (save ? "  <- A STRANGER CAN FILL REDIS" : ""));
            }
            redis.cli(true, "flushall");
        }
        line(out, "  THE PRICE:");
        line(out, "    a customer who stops to read a statement signs in again");
        line(out, "    and every request now needs Redis to answer");
    }

    /** A Set-Cookie header, wrapped at its attributes so no line is too long to read. */
    private static void header(StringBuilder out, String value) {
        String[] parts = value.split("; ");
        StringBuilder row = new StringBuilder("    ").append(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (row.length() + parts[i].length() + 2 > 64) {
                line(out, row + ";");
                row = new StringBuilder("        ").append(parts[i]);
            } else {
                row.append("; ").append(parts[i]);
            }
        }
        line(out, row.toString());
    }
}
