package com.jk.explore.security.sessions;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Runs the online bank story in five numbered acts and prints exact, repeatable results. Session ids are random, so none is ever printed. */
public class SessionDemo {

    static final String ANA = "ana";
    static final String PASSWORD = "demo-password-ana";

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() throws IOException, InterruptedException {
        StringBuilder out = new StringBuilder();
        act1Ids(out);
        act2Rotation(out);
        act3Lifetimes(out);
        act4SignOut(out);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    static Users users() {
        Users users = new Users();
        users.set(ANA, PASSWORD.toCharArray());
        return users;
    }

    private static SessionManager bank(SimClock clock, Settings settings) {
        return new SessionManager(clock, settings, new SessionStore(), users());
    }

    private static String signIn(SessionManager bank, String current) {
        return bank.signIn(current, ANA, PASSWORD.toCharArray()).orElseThrow();
    }

    private static void act1Ids(StringBuilder out) {
        line(out, "ONE. A session id: random, unguessable, and never stored as itself.");
        String id = SessionIds.newId();
        line(out, "  a new session id is " + SessionIds.BITS + " bits from SecureRandom, written as " + id.length() + " base64url characters: "
                + id.matches("[A-Za-z0-9_-]{22}"));
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(SessionIds.newId());
        }
        line(out, "  1000 new ids, all different: " + (ids.size() == 1000));
        SessionManager bank = bank(new SimClock(), Settings.bank());
        List<String> issued = List.of(signIn(bank, null), bank.start(), bank.start());
        line(out, "  ana's id contains her name: " + issued.get(0).contains(ANA) + "; it is made of randomness only, with no time or counter in it");
        long live = bank.store().keys().stream().filter(key -> bank.account(key) != Answer.UNKNOWN).count();
        line(out, "  the store keeps the SHA-256 of each id; a stolen copy of its " + bank.store().size() + " keys, sent as cookies, opens "
                + live + " sessions");
        line(out, "  ana's real id reads her account: " + bank.account(issued.get(0)).text());
        String changed = issued.get(0).substring(0, 21) + (issued.get(0).charAt(21) == 'A' ? 'B' : 'A');
        line(out, "  the same id with its last character changed: " + bank.account(changed).text());
        line(out, "  the stored hash is compared in constant time, and ids are never printed or logged: this output shows none");
    }

    private static void act2Rotation(StringBuilder out) {
        line(out, "TWO. A new id at every sign-in and step-up.");
        SessionManager naive = bank(new SimClock(), Settings.withoutRotation());
        String planted = naive.start();
        line(out, "  a stranger opens the bank's site, gets an id for a visitor who has not signed in, and gets it into ana's browser");
        String after = signIn(naive, planted);
        line(out, "  a bank that keeps the id at sign-in: ana's id after signing in is the planted one: " + after.equals(planted));
        line(out, "  the stranger's copy now reads ana's account: " + naive.account(planted).text() + "  <- SESSION FIXATION");
        SessionManager bank = bank(new SimClock(), Settings.bank());
        String plantedAgain = bank.start();
        String fresh = signIn(bank, plantedAgain);
        line(out, "  a bank that issues a new id at sign-in: the ids differ: " + !fresh.equals(plantedAgain));
        line(out, "  the stranger's copy: " + bank.account(plantedAgain).text() + ", and ana's new id: " + bank.account(fresh).text());
        String stepped = bank.stepUp(fresh, PASSWORD.toCharArray()).orElseThrow();
        line(out, "  before a payment ana gives her password again, and the id changes again: " + !stepped.equals(fresh));
        line(out, "  the id from before the step-up: " + bank.account(fresh).text() + ", the new one pays: " + bank.payment(stepped).text());
    }

    private static void act3Lifetimes(StringBuilder out) {
        line(out, "THREE. How long a session lives.");
        Settings settings = Settings.bank();
        line(out, "  idle timeout " + settings.idleSeconds() / 60 + " minutes, absolute timeout " + settings.absoluteSeconds() / 3600
                + " hours, the password again for a payment after " + settings.reauthSeconds() / 60 + " minutes");
        SimClock clock = new SimClock();
        SessionManager bank = bank(clock, settings);
        String id = signIn(bank, null);
        clock.advance(SimClock.minutes(14));
        line(out, "  ana is idle for 14 minutes: " + bank.account(id).text());
        clock.advance(SimClock.minutes(16));
        line(out, "  then idle for 16 minutes: " + bank.account(id).text());
        line(out, "  and that id stays dead: " + bank.account(id).text());

        SimClock busy = new SimClock();
        SessionManager open = bank(busy, settings);
        String working = signIn(open, null);
        int allowed = 0;
        Answer last = Answer.ALLOWED;
        while (last == Answer.ALLOWED) {
            busy.advance(SimClock.minutes(10));
            last = open.account(working);
            if (last == Answer.ALLOWED) {
                allowed++;
            }
        }
        line(out, "  ana stays busy, a request every 10 minutes: " + allowed + " requests allowed, then at " + busy.now() / 3600 + " hours: "
                + last.text());

        SimClock pay = new SimClock();
        SessionManager payments = bank(pay, settings);
        String payer = signIn(payments, null);
        pay.advance(SimClock.minutes(3));
        line(out, "  a payment 3 minutes after signing in: " + payments.payment(payer).text());
        pay.advance(SimClock.minutes(3));
        line(out, "  a payment 6 minutes after signing in: " + payments.payment(payer).text() + "; reading the account: "
                + payments.account(payer).text());
    }

    private static void act4SignOut(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. Signing out, everywhere, and the cookie.");
        SessionManager shared = bank(new SimClock(), Settings.bank());
        try (BankServer server = new BankServer(shared); BankClient client = new BankClient(server.port())) {
            BankClient.Reply in = client.signIn(ANA, PASSWORD);
            line(out, "  sign-in on a real local server: " + in.status() + ", cookie " + Cookies.NAME + " with Path=/, Secure, HttpOnly, SameSite=Strict: problems "
                    + Cookies.problems(in.setCookie()).size());
            line(out, "  the account page with that cookie: " + client.account().status());
            String copied = client.browser.get(Cookies.NAME).orElseThrow();
            BankClient.Reply signedOut = client.signOut();
            line(out, "  sign-out: " + signedOut.status() + ", and the browser is told to forget the cookie: " + client.browser.get(Cookies.NAME).isEmpty());
            client.browser.put(Cookies.NAME, copied);
            line(out, "  a copy of the cookie made before sign-out, sent again: " + client.account().status()
                    + "  <- THE SERVER ENDED THE SESSION, NOT ONLY THE BROWSER");
        }
        SessionManager forgetful = bank(new SimClock(), Settings.bank());
        Browser browser = new Browser();
        String id = signIn(forgetful, null);
        browser.put(Cookies.NAME, id);
        browser.clear(Cookies.NAME);
        line(out, "  a bank whose sign-out only clears the browser's cookie: the copy still reads the account: " + forgetful.account(id).text()
                + "  <- STILL SIGNED IN");
        line(out, "  a cookie named SESSIONID with no attributes has " + Cookies.problems("SESSIONID=" + id).size() + " problems: "
                + String.join(", ", Cookies.problems("SESSIONID=" + id)));

        SessionManager devices = bank(new SimClock(), Settings.bank());
        signIn(devices, null);
        signIn(devices, null);
        signIn(devices, null);
        line(out, "  ana is signed in on her phone, laptop and tablet; sign out everywhere ends " + devices.signOutEverywhere(ANA) + " sessions");

        SessionManager change = bank(new SimClock(), Settings.bank());
        signIn(change, null);
        signIn(change, null);
        String laptop = signIn(change, null);
        SessionManager.Changed changed = change.changePassword(laptop, PASSWORD.toCharArray(), "demo-password-ana-2".toCharArray()).orElseThrow();
        line(out, "  ana changes her password on the laptop: " + changed.ended() + " other sessions ended, the laptop goes on with a new id: "
                + change.account(changed.id()).text() + ", the old laptop id: " + change.account(laptop).text());

        SessionManager limited = bank(new SimClock(), Settings.bank());
        String phone = signIn(limited, null);
        signIn(limited, null);
        signIn(limited, null);
        signIn(limited, null);
        line(out, "  a limit of " + Settings.bank().maxSessions() + " sessions: a fourth sign-in ends the oldest, the phone's: " + limited.account(phone).text());
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill.");
        Users users = users();
        SimClock clock = new SimClock();
        SessionManager serverA = new SessionManager(clock, Settings.bank(), new SessionStore(), users);
        SessionManager serverB = new SessionManager(clock, Settings.bank(), new SessionStore(), users);
        String id = serverA.signIn(null, ANA, PASSWORD.toCharArray()).orElseThrow();
        line(out, "  two servers, each with its own session memory: ana signs in on A, her next request lands on B: " + serverB.account(id).text()
                + "  <- NEEDS A SHARED STORE");
        SessionStore shared = new SessionStore();
        SessionManager sharedA = new SessionManager(clock, Settings.bank(), shared, users);
        SessionManager sharedB = new SessionManager(clock, Settings.bank(), shared, users);
        String both = sharedA.signIn(null, ANA, PASSWORD.toCharArray()).orElseThrow();
        line(out, "  with one store shared by both servers: " + sharedB.account(both).text() + "; the -with-spring-session-and-redis version keeps it in Redis");
        SimClock reading = new SimClock();
        SessionManager slow = bank(reading, Settings.bank());
        String reader = signIn(slow, null);
        reading.advance(SimClock.minutes(20));
        line(out, "  a customer who stops for 20 minutes to read a statement: " + slow.account(reader).text() + "  <- THE PRICE OF A SHORT TIMEOUT");
        line(out, "  a stolen id works only until it is rotated, times out or is ended, which is what these rules limit: at most 15 idle minutes or 8 hours here");
        Optional<String> wrong = slow.signIn(null, ANA, "not-the-password".toCharArray());
        line(out, "  and a session only starts after the right password: a wrong one gives a session: " + wrong.isPresent());
    }
}
