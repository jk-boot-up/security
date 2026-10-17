package com.jk.explore.security.auth;

import java.util.Optional;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. */
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
        act3LoginIsNotEnough(out);
        act4Authorization(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1NoChecks(StringBuilder out) {
        line(out, "ONE. The first version, with no checks.");
        Shop shop = new World().shop(new NoGate());
        line(out, "  nobody refunds O-1: " + shop.handle(null, Action.REFUND, "O-1").status() + "  <- NOBODY ASKED WHO");
        line(out, "  nobody views O-1: " + shop.handle(null, Action.VIEW, "O-1").status());
    }

    private static void act2Authentication(StringBuilder out) {
        line(out, "TWO. Authentication: finding out who is asking.");
        World w = new World();
        Shop shop = w.shop(new LoginOnlyGate(w.authenticator));
        Optional<Session> wrong = w.authenticator.login("ben", "guess");
        line(out, "  ben logs in with a wrong password: " + (wrong.isPresent() ? "accepted" : "rejected"));
        String token = w.login("ben");
        line(out, "  ben logs in with his password: session " + token);
        line(out, "  request with no token: " + shop.handle(null, Action.VIEW, "O-2").status());
        line(out, "  request with a made-up token tok-99: " + shop.handle("tok-99", Action.VIEW, "O-2").status());
        line(out, "  request with ben's token: " + shop.handle(token, Action.VIEW, "O-2").status());
    }

    private static void act3LoginIsNotEnough(StringBuilder out) {
        line(out, "THREE. Authenticated is not the same as authorized.");
        World w = new World();
        Shop shop = w.shop(new LoginOnlyGate(w.authenticator));
        String ben = w.login("ben");
        line(out, "  ben, a customer, refunds O-1: " + shop.handle(ben, Action.REFUND, "O-1").status()
                + "  <- LOGGED IN, SO ALLOWED");
        line(out, "  ben views ana's order O-1: " + shop.handle(ben, Action.VIEW, "O-1").status()
                + "  <- NOT HIS ORDER");
    }

    private static void act4Authorization(StringBuilder out) {
        line(out, "FOUR. Authorization: what may this person do?");
        World w = new World();
        Shop shop = w.shop(new PolicyGate(w.authenticator, w.directory, PolicyGate.RoleSource.SESSION));
        String ben = w.login("ben");
        String ana = w.login("ana");
        String sam = w.login("sam");
        String alex = w.login("alex");
        line(out, "  no token refunds O-1: " + shop.handle(null, Action.REFUND, "O-1").status());
        line(out, "  ben refunds O-1: " + shop.handle(ben, Action.REFUND, "O-1").status());
        line(out, "  ben views ana's order O-1: " + shop.handle(ben, Action.VIEW, "O-1").status());
        line(out, "  ben views his own order O-2: " + shop.handle(ben, Action.VIEW, "O-2").status());
        line(out, "  sam, from support, views O-1: " + shop.handle(sam, Action.VIEW, "O-1").status());
        line(out, "  sam refunds O-1: " + shop.handle(sam, Action.REFUND, "O-1").status());
        line(out, "  ana cancels ben's order O-2: " + shop.handle(ana, Action.CANCEL, "O-2").status());
        line(out, "  alex, an admin, cancels O-2: " + shop.handle(alex, Action.CANCEL, "O-2").status());
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: sessions expire, and roles go stale.");
        World w = new World();
        Shop copied = w.shop(new PolicyGate(w.authenticator, w.directory, PolicyGate.RoleSource.SESSION));
        String sam = w.login("sam");
        line(out, "  sam logs in at tick 0: " + sam + ", good for " + SESSION_LIFETIME + " ticks");
        w.ticker.advance(4);
        line(out, "  at tick 4 sam refunds O-1: " + copied.handle(sam, Action.REFUND, "O-1").status() + " (expired)");

        World w2 = new World();
        Shop fromSession = w2.shop(new PolicyGate(w2.authenticator, w2.directory, PolicyGate.RoleSource.SESSION));
        Shop fromDirectory = w2.shop(new PolicyGate(w2.authenticator, w2.directory, PolicyGate.RoleSource.DIRECTORY));
        String sam2 = w2.login("sam");
        w2.directory.changeRole("sam", Role.CUSTOMER);
        line(out, "  sam logs in, then alex demotes sam to CUSTOMER");
        line(out, "  sam refunds with the role copied at login: " + fromSession.handle(sam2, Action.REFUND, "O-1").status()
                + "  <- STALE ROLE");
        int before = w2.directory.lookups();
        line(out, "  sam refunds with the role looked up each time: " + fromDirectory.handle(sam2, Action.REFUND, "O-1").status());
        line(out, "  role lookups for that one request: " + (w2.directory.lookups() - before));
    }
}
