package com.jk.explore.security.auth;

import java.util.HexFormat;
import java.util.Optional;

/**
 * Runs the online store story in seven numbered acts and prints exact, repeatable results. Each act prints
 * what goes in (THE REQUESTS, or BEFORE ...) and what comes out (THE RESPONSES, or AFTER ...) in separate sections.
 */
public class AuthDemo {

    static final Order ANAS_LAPTOP = new Order("O-1", "ana", "Laptop", 1200);
    static final Order BENS_MOUSE = new Order("O-2", "ben", "Mouse", 25);
    static final int SESSION_LIFETIME = 3;

    /** Everything one act needs, built fresh so the acts do not affect each other. */
    static final class World {
        final Ticker ticker = new Ticker();
        final UserDirectory directory = new UserDirectory()
                .add(new Account("ana", Role.CUSTOMER))
                .add(new Account("ben", Role.CUSTOMER))
                .add(new Account("sam", Role.SUPPORT))
                .add(new Account("alex", Role.ADMIN));
        final Credentials credentials = new Credentials()
                .add("ana", "ana-pass").add("ben", "ben-pass").add("sam", "sam-pass").add("alex", "alex-pass");
        final Authenticator authenticator = new Authenticator(credentials, directory, ticker, SESSION_LIFETIME);

        Shop shop(Gate gate) {
            return new Shop(gate).add(ANAS_LAPTOP).add(BENS_MOUSE);
        }

        String login(String user) {
            return authenticator.login(user, user + "-pass").orElseThrow().token();
        }
    }

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1NoChecks(out);
        act2Authentication(out);
        act3PasswordCheck(out);
        act4LoginIsNotEnough(out);
        act5Authorization(out);
        act6HowTheGateDecides(out);
        act7Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    /** "a1b2c3d4 e5f6a7b8" for the given bytes of a hash, in groups of four. */
    static String hex(byte[] bytes, int from, int to) {
        String h = HexFormat.of().formatHex(bytes, from, to);
        return h.replaceAll("(.{8})(?!$)", "$1 ");
    }

    private static void act1NoChecks(StringBuilder out) {
        line(out, "ONE. The first version, with no checks.");
        Shop shop = new World().shop(new NoGate());
        line(out, "  THE REQUESTS (no login, no token):");
        line(out, "    1  refund O-1  (ana's laptop, 1200 dollars)");
        line(out, "    2  view O-1");
        line(out, "  THE RESPONSES:");
        line(out, "    1  " + shop.handle(null, Action.REFUND, "O-1").status() + "  <- NOBODY ASKED WHO");
        line(out, "    2  " + shop.handle(null, Action.VIEW, "O-1").status());
    }

    private static void act2Authentication(StringBuilder out) {
        line(out, "TWO. Authentication: finding out who is asking.");
        World w = new World();
        Shop shop = w.shop(new LoginOnlyGate(w.authenticator));
        line(out, "  THE REQUESTS:");
        line(out, "    1  log in: ben, password guess");
        line(out, "    2  log in: ben, password ben-pass");
        line(out, "    3  view O-2 with no token");
        line(out, "    4  view O-2 with a made-up token tok-99");
        line(out, "    5  view O-2 with ben's token tok-1");
        Optional<Session> wrong = w.authenticator.login("ben", "guess");
        Session ben = w.authenticator.login("ben", "ben-pass").orElseThrow();
        line(out, "  THE RESPONSES:");
        line(out, "    1  " + (wrong.isPresent() ? "accepted" : "rejected") + "  <- WRONG PASSWORD");
        line(out, "    2  session " + ben.token() + ": " + ben.username() + ", " + ben.roleAtLogin()
                + ", issued at tick " + ben.issuedAt());
        line(out, "    3  " + shop.handle(null, Action.VIEW, "O-2").status() + "  <- WHO ARE YOU?");
        line(out, "    4  " + shop.handle("tok-99", Action.VIEW, "O-2").status() + "  <- NEVER ISSUED");
        line(out, "    5  " + shop.handle(ben.token(), Action.VIEW, "O-2").status() + "  <- IT IS BEN");
    }

    private static void act3PasswordCheck(StringBuilder out) {
        line(out, "THREE. How a password is checked, step by step.");
        World w = new World();
        byte[] stored = w.credentials.storedHash("ben");
        line(out, "  BEFORE HASHING (when ben signed up):");
        line(out, "    password     ben-pass");
        line(out, "    salt         " + Credentials.salt("ben"));
        line(out, "    rounds       " + Credentials.ITERATIONS + " of PBKDF2 with HMAC-SHA256");
        line(out, "  AFTER HASHING (all the shop keeps, " + stored.length + " bytes):");
        line(out, "    bytes 1-16   " + hex(stored, 0, 16));
        line(out, "    bytes 17-32  " + hex(stored, 16, 32));
        line(out, "  AT LOGIN, THE TYPED PASSWORD IS HASHED THE SAME WAY:");
        for (String typed : new String[] {"guess", "ben-pass"}) {
            byte[] attempt = Credentials.hash("ben", typed);
            boolean same = w.credentials.verify("ben", typed);
            line(out, "    " + (typed + "        ").substring(0, 9) + "    " + hex(attempt, 0, 8) + " ...  "
                    + (same ? "<- THE SAME: ACCEPTED" : "<- DIFFERENT: REJECTED"));
        }
        line(out, "  the password itself is stored nowhere");
    }

    private static void act4LoginIsNotEnough(StringBuilder out) {
        line(out, "FOUR. Authenticated is not the same as authorized.");
        World w = new World();
        Shop shop = w.shop(new LoginOnlyGate(w.authenticator));
        String ben = w.login("ben");
        line(out, "  THE REQUESTS (ben, a customer, token " + ben + "):");
        line(out, "    1  refund O-1  (ana's order)");
        line(out, "    2  view O-1    (ana's order)");
        line(out, "  THE RESPONSES (the gate asks only: logged in?):");
        line(out, "    1  " + shop.handle(ben, Action.REFUND, "O-1").status() + "  <- LOGGED IN, SO ALLOWED");
        line(out, "    2  " + shop.handle(ben, Action.VIEW, "O-1").status() + "  <- NOT HIS ORDER");
    }

    private static void act5Authorization(StringBuilder out) {
        line(out, "FIVE. Authorization: what may this person do?");
        World w = new World();
        Shop shop = w.shop(new PolicyGate(w.authenticator, w.directory, PolicyGate.RoleSource.SESSION));
        String ben = w.login("ben");
        String ana = w.login("ana");
        String sam = w.login("sam");
        String alex = w.login("alex");
        line(out, "  THE REQUESTS:");
        line(out, "    1  no token        refund O-1");
        line(out, "    2  ben, customer   refund O-1");
        line(out, "    3  ben, customer   view O-1  (ana's)");
        line(out, "    4  ben, customer   view O-2  (his own)");
        line(out, "    5  sam, support    view O-1");
        line(out, "    6  sam, support    refund O-1");
        line(out, "    7  ana, customer   cancel O-2  (ben's)");
        line(out, "    8  alex, admin     cancel O-2");
        line(out, "  THE RESPONSES:");
        line(out, "    1  " + shop.handle(null, Action.REFUND, "O-1").status() + "  <- NOBODY KNOWN");
        line(out, "    2  " + shop.handle(ben, Action.REFUND, "O-1").status() + "  <- KNOWN, NOT ALLOWED");
        line(out, "    3  " + shop.handle(ben, Action.VIEW, "O-1").status());
        line(out, "    4  " + shop.handle(ben, Action.VIEW, "O-2").status());
        line(out, "    5  " + shop.handle(sam, Action.VIEW, "O-1").status());
        line(out, "    6  " + shop.handle(sam, Action.REFUND, "O-1").status());
        line(out, "    7  " + shop.handle(ana, Action.CANCEL, "O-2").status());
        line(out, "    8  " + shop.handle(alex, Action.CANCEL, "O-2").status());
    }

    /** "any", "own only" or "no": what the rules in PolicyGate.allowed say for one role and action. */
    static String rule(Role role, Action action) {
        boolean own = PolicyGate.allowed(role, "ana", action, ANAS_LAPTOP);
        boolean other = PolicyGate.allowed(role, "ben", action, ANAS_LAPTOP);
        return other ? "any" : own ? "own only" : "no";
    }

    private static void act6HowTheGateDecides(StringBuilder out) {
        line(out, "SIX. How the gate decides, step by step.");
        World w = new World();
        Shop shop = w.shop(new PolicyGate(w.authenticator, w.directory, PolicyGate.RoleSource.SESSION));
        String token = w.login("ben");
        line(out, "  THE RULES (from PolicyGate.allowed):");
        line(out, "    role        VIEW        CANCEL      REFUND");
        for (Role role : Role.values()) {
            line(out, String.format("    %-12s%-12s%-12s%s", role, rule(role, Action.VIEW),
                    rule(role, Action.CANCEL), rule(role, Action.REFUND)).stripTrailing());
        }
        line(out, "  THE REQUEST:");
        line(out, "    token " + token + ", view O-1");
        Session session = w.authenticator.authenticate(token).orElseThrow();
        int age = w.ticker.now() - session.issuedAt();
        line(out, "  STEP 1. AUTHENTICATION: WHO IS THIS?");
        line(out, "    session " + token + "  " + session.username() + ", " + session.roleAtLogin()
                + ", issued at tick " + session.issuedAt());
        line(out, "    age " + age + " ticks, under " + SESSION_LIFETIME + ": live, so it is " + session.username());
        line(out, "  STEP 2. AUTHORIZATION: MAY BEN DO THIS?");
        line(out, "    rule for " + session.roleAtLogin() + ", VIEW: " + rule(session.roleAtLogin(), Action.VIEW));
        line(out, "    owner of O-1: " + ANAS_LAPTOP.owner() + ", not " + session.username());
        line(out, "  THE RESPONSE:");
        line(out, "    " + shop.handle(token, Action.VIEW, "O-1").status() + "  <- KNOWN, NOT ALLOWED");
    }

    private static void act7Bill(StringBuilder out) {
        line(out, "SEVEN. The bill: sessions expire, and roles go stale.");
        World w = new World();
        Shop copied = w.shop(new PolicyGate(w.authenticator, w.directory, PolicyGate.RoleSource.SESSION));
        Session sam = w.authenticator.login("sam", "sam-pass").orElseThrow();
        line(out, "  SESSIONS EXPIRE:");
        line(out, "    sam logs in at tick " + sam.issuedAt() + ": " + sam.token() + ", lifetime "
                + SESSION_LIFETIME + " ticks");
        w.ticker.advance(4);
        int age = w.ticker.now() - sam.issuedAt();
        line(out, "    at tick " + w.ticker.now() + " the age is " + age + ", not under " + SESSION_LIFETIME
                + ": expired");
        line(out, "    sam refunds O-1: " + copied.handle(sam.token(), Action.REFUND, "O-1").status());

        World w2 = new World();
        Shop fromSession = w2.shop(new PolicyGate(w2.authenticator, w2.directory, PolicyGate.RoleSource.SESSION));
        Shop fromDirectory = w2.shop(new PolicyGate(w2.authenticator, w2.directory, PolicyGate.RoleSource.DIRECTORY));
        Session sam2 = w2.authenticator.login("sam", "sam-pass").orElseThrow();
        line(out, "  BEFORE THE DEMOTION:");
        line(out, "    sam logs in: session " + sam2.token() + " holds role " + sam2.roleAtLogin());
        w2.directory.changeRole("sam", Role.CUSTOMER);
        line(out, "  AFTER THE DEMOTION (alex makes sam a CUSTOMER):");
        int before = w2.directory.lookups();
        Status stale = fromSession.handle(sam2.token(), Action.REFUND, "O-1").status();
        int copiedLookups = w2.directory.lookups() - before;
        line(out, "    refund, role copied at login: " + stale + "  <- STALE");
        before = w2.directory.lookups();
        Status current = fromDirectory.handle(sam2.token(), Action.REFUND, "O-1").status();
        int lookedUpLookups = w2.directory.lookups() - before;
        line(out, "    refund, role looked up now:   " + current);
        line(out, "    directory lookups: " + copiedLookups + " for the first, " + lookedUpLookups
                + " for the second");
    }
}
