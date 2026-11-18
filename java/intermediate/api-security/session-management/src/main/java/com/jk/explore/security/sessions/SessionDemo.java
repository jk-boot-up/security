package com.jk.explore.security.sessions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Runs the online bank story in five numbered acts and prints exact, repeatable results. Each act prints what goes in and what comes out in
 * separate, labelled sections. Session ids are random, so none is ever printed: they appear as placeholders such as {@code <planted>}.
 */
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

    /** Sixteen example bytes, fixed so the steps print the same on every run. Real ids take their bytes from SecureRandom. */
    static final String EXAMPLE_BYTES = "4f1a9c27d3e8b05a6c1f72e9a4d03b8e";

    private static void act1Ids(StringBuilder out) {
        line(out, "ONE. A session id: random, and never stored as itself.");
        byte[] bytes = HexFormat.of().parseHex(EXAMPLE_BYTES);
        String example = SessionIds.encode(bytes);
        line(out, "  BEFORE ENCODING (16 example bytes; real ones are random):");
        line(out, "    " + spaced(EXAMPLE_BYTES) + "  (" + bytes.length * 8 + " bits)");
        line(out, "  HOW BASE64URL WORKS (the first 3 bytes, 24 bits):");
        byte[] first = {bytes[0], bytes[1], bytes[2]};
        line(out, "    " + String.join(" ", Base64Steps.byteBits(first)));
        List<String> groups = Base64Steps.sixBitGroups(first);
        List<String> picked = new ArrayList<>();
        for (String group : groups) {
            picked.add(group + "=" + SessionIds.ALPHABET.charAt(Integer.parseInt(group, 2)));
        }
        line(out, "    cut into 6 bits a character: " + String.join(" ", picked));
        line(out, "  AFTER ENCODING (the id the browser keeps):");
        line(out, "    " + example + "  (" + example.length() + " characters)");
        line(out, "  WHAT THE STORE KEEPS (the SHA-256 of the id, in hex):");
        line(out, "    " + SessionIds.key(SessionIds.hash(example)));

        String id = SessionIds.newId();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(SessionIds.newId());
        }
        SessionManager bank = bank(new SimClock(), Settings.bank());
        List<String> issued = List.of(signIn(bank, null), bank.start(), bank.start());
        line(out, "  THE REAL IDS (from SecureRandom, never printed):");
        line(out, "    a new id is 22 base64url characters: " + id.matches("[A-Za-z0-9_-]{22}"));
        line(out, "    1000 new ids, all different: " + (ids.size() == 1000));
        line(out, "    ana's id contains her name: " + issued.get(0).contains(ANA) + " (no name, time or counter)");
        long live = bank.store().keys().stream().filter(key -> bank.account(key) != Answer.UNKNOWN).count();
        line(out, "  THE REQUEST (a stolen copy of the store's " + bank.store().size() + " keys, as cookies):");
        line(out, "  THE ANSWER: " + live + " sessions opened");
        String changed = issued.get(0).substring(0, 21) + (issued.get(0).charAt(21) == 'A' ? 'B' : 'A');
        line(out, "  THE REQUEST (ana's id, then one character changed):");
        line(out, "  THE ANSWERS:");
        line(out, "    <ana's id>    " + bank.account(issued.get(0)).text());
        line(out, "    <changed id>  " + bank.account(changed).text());
        line(out, "  hashes are compared in constant time; ids are never logged");
    }

    private static void act2Rotation(StringBuilder out) {
        line(out, "TWO. A new id at every sign-in and step-up.");
        SessionManager naive = bank(new SimClock(), Settings.withoutRotation());
        String planted = naive.start();
        line(out, "  BEFORE ANA SIGNS IN:");
        line(out, "    a stranger asks the bank for a visitor id: <planted>");
        line(out, "    and gets <planted> into ana's browser");
        String after = signIn(naive, planted);
        line(out, "  AFTER SIGN-IN, A BANK THAT KEEPS THE ID:");
        line(out, "    ana's id is still <planted>: " + after.equals(planted));
        line(out, "    the stranger sends <planted>: " + naive.account(planted).text() + "  <- SESSION FIXATION");
        SessionManager bank = bank(new SimClock(), Settings.bank());
        String plantedAgain = bank.start();
        String fresh = signIn(bank, plantedAgain);
        line(out, "  AFTER SIGN-IN, A BANK THAT ISSUES A NEW ID:");
        line(out, "    ana's id is still <planted>: " + fresh.equals(plantedAgain) + "; she now has <new>");
        line(out, "    the stranger sends <planted>: " + bank.account(plantedAgain).text());
        line(out, "    ana sends <new>: " + bank.account(fresh).text());
        String stepped = bank.stepUp(fresh, PASSWORD.toCharArray()).orElseThrow();
        line(out, "  AFTER STEP-UP (the password again before a payment):");
        line(out, "    ana's id is still <new>: " + stepped.equals(fresh) + "; she now has <newer>");
        line(out, "    <new> is sent: " + bank.account(fresh).text());
        line(out, "    <newer> makes a payment: " + bank.payment(stepped).text());
    }

    private static void act3Lifetimes(StringBuilder out) {
        line(out, "THREE. How long a session lives.");
        Settings settings = Settings.bank();
        line(out, "  THE SETTINGS:");
        line(out, "    idle timeout       " + settings.idleSeconds() / 60 + " minutes without a request");
        line(out, "    absolute timeout   " + settings.absoluteSeconds() / 3600 + " hours after sign-in, busy or not");
        line(out, "    password again     for a payment, " + settings.reauthSeconds() / 60 + " minutes after it");
        SimClock clock = new SimClock();
        SessionManager bank = bank(clock, settings);
        String id = signIn(bank, null);
        line(out, "  IDLE (minutes since ana's last request -> the answer):");
        clock.advance(SimClock.minutes(14));
        line(out, "    14 -> " + bank.account(id).text());
        clock.advance(SimClock.minutes(16));
        line(out, "    16 -> " + bank.account(id).text());
        line(out, "    the same id, any time later -> " + bank.account(id).text());

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
        long absoluteMinutes = settings.absoluteSeconds() / 60;
        line(out, "  ABSOLUTE (ana stays busy, a request every 10 minutes):");
        line(out, "    " + settings.absoluteSeconds() / 3600 + " hours = " + absoluteMinutes + " minutes; " + absoluteMinutes + " / 10 = request "
                + absoluteMinutes / 10 + " is at " + clockTime(busy.now()));
        line(out, "    requests 1 to " + allowed + " (0:10 to " + clockTime(busy.now() - SimClock.minutes(10)) + ") -> allowed");
        line(out, "    request " + (allowed + 1) + " (" + clockTime(busy.now()) + ") -> " + last.text());

        SimClock pay = new SimClock();
        SessionManager payments = bank(pay, settings);
        String payer = signIn(payments, null);
        line(out, "  PAYMENTS (minutes since the password was given -> the answer):");
        pay.advance(SimClock.minutes(3));
        line(out, "    3 -> payment " + payments.payment(payer).text());
        pay.advance(SimClock.minutes(3));
        line(out, "    6 -> payment " + payments.payment(payer).text());
        line(out, "    6 -> reading the account " + payments.account(payer).text());
    }

    private static void act4SignOut(StringBuilder out) throws IOException, InterruptedException {
        line(out, "FOUR. Signing out, everywhere, and the cookie.");
        SessionManager shared = bank(new SimClock(), Settings.bank());
        try (BankServer server = new BankServer(shared); BankClient client = new BankClient(server.port())) {
            BankClient.Reply in = client.signIn(ANA, PASSWORD);
            String id = client.browser.get(Cookies.NAME).orElseThrow();
            line(out, "  THE SIGN-IN RESPONSE (a real server on this machine):");
            line(out, "    status " + in.status());
            header(out, "Set-Cookie: " + in.setCookie().replace(id, "<id>"));
            line(out, "    problems found: " + Cookies.problems(in.setCookie()).size());
            line(out, "  THE ACCOUNT PAGE, WITH THAT COOKIE: status " + client.account().status());
            BankClient.Reply signedOut = client.signOut();
            line(out, "  THE SIGN-OUT RESPONSE:");
            line(out, "    status " + signedOut.status());
            header(out, "Set-Cookie: " + signedOut.setCookie());
            line(out, "    the browser forgets the cookie: " + client.browser.get(Cookies.NAME).isEmpty());
            client.browser.put(Cookies.NAME, id);
            line(out, "  A COPY MADE BEFORE SIGN-OUT, SENT AGAIN:");
            line(out, "    status " + client.account().status() + "  <- THE SERVER ENDED THE SESSION");
        }
        SessionManager forgetful = bank(new SimClock(), Settings.bank());
        Browser browser = new Browser();
        String id = signIn(forgetful, null);
        browser.put(Cookies.NAME, id);
        browser.clear(Cookies.NAME);
        line(out, "  A BANK WHOSE SIGN-OUT ONLY CLEARS THE BROWSER'S COOKIE:");
        line(out, "    the copy reads the account: " + forgetful.account(id).text() + "  <- STILL SIGNED IN");
        String weak = "SESSIONID=" + id;
        List<String> problems = Cookies.problems(weak);
        line(out, "  A WEAK COOKIE:");
        header(out, "Set-Cookie: " + weak.replace(id, "<id>"));
        line(out, "  ITS PROBLEMS (" + problems.size() + "):");
        line(out, "    " + String.join(", ", problems));

        SessionManager devices = bank(new SimClock(), Settings.bank());
        signIn(devices, null);
        signIn(devices, null);
        signIn(devices, null);
        line(out, "  ANA IS SIGNED IN ON HER PHONE, LAPTOP AND TABLET:");
        line(out, "    sign out everywhere -> " + devices.signOutEverywhere(ANA) + " sessions ended");

        SessionManager change = bank(new SimClock(), Settings.bank());
        signIn(change, null);
        signIn(change, null);
        String laptop = signIn(change, null);
        SessionManager.Changed changed = change.changePassword(laptop, PASSWORD.toCharArray(), "demo-password-ana-2".toCharArray()).orElseThrow();
        line(out, "    a password change on the laptop -> " + changed.ended() + " other sessions ended");
        line(out, "      the laptop's new id: " + change.account(changed.id()).text());
        line(out, "      the laptop's old id: " + change.account(laptop).text());

        SessionManager limited = bank(new SimClock(), Settings.bank());
        String phone = signIn(limited, null);
        signIn(limited, null);
        signIn(limited, null);
        signIn(limited, null);
        line(out, "    a limit of " + Settings.bank().maxSessions() + "; a fourth sign-in ends the oldest, the phone's");
        line(out, "      the phone's id: " + limited.account(phone).text());
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill.");
        Users users = users();
        SimClock clock = new SimClock();
        SessionManager serverA = new SessionManager(clock, Settings.bank(), new SessionStore(), users);
        SessionManager serverB = new SessionManager(clock, Settings.bank(), new SessionStore(), users);
        String id = serverA.signIn(null, ANA, PASSWORD.toCharArray()).orElseThrow();
        line(out, "  THE REQUEST: ana signs in on server A; her next request lands on B");
        line(out, "  THE ANSWER FROM B, EACH SERVER WITH ITS OWN MEMORY:");
        line(out, "    " + serverB.account(id).text() + "  <- NEEDS A SHARED STORE");
        SessionStore shared = new SessionStore();
        SessionManager sharedA = new SessionManager(clock, Settings.bank(), shared, users);
        SessionManager sharedB = new SessionManager(clock, Settings.bank(), shared, users);
        String both = sharedA.signIn(null, ANA, PASSWORD.toCharArray()).orElseThrow();
        line(out, "  THE ANSWER FROM B, BOTH SERVERS SHARING ONE STORE:");
        line(out, "    " + sharedB.account(both).text() + " (the -with-spring-session-and-redis version)");
        SimClock reading = new SimClock();
        SessionManager slow = bank(reading, Settings.bank());
        String reader = signIn(slow, null);
        reading.advance(SimClock.minutes(20));
        line(out, "  THE REQUEST: a customer reads a statement for 20 minutes");
        line(out, "  THE ANSWER:");
        line(out, "    " + slow.account(reader).text() + "  <- THE PRICE");
        Settings settings = Settings.bank();
        line(out, "  HOW LONG A STOLEN ID WORKS, AT MOST:");
        line(out, "    " + settings.idleSeconds() / 60 + " idle minutes, " + settings.absoluteSeconds() / 3600
                + " hours in all, or until rotated or ended");
        Optional<String> wrong = slow.signIn(null, ANA, "not-the-password".toCharArray());
        line(out, "  A WRONG PASSWORD STARTS A SESSION: " + wrong.isPresent());
    }

    /** Hex bytes with a space between each pair, as people read them. */
    static String spaced(String hex) {
        return String.join(" ", hex.split("(?<=\\G..)"));
    }

    /** Seconds since sign-in as hours:minutes. */
    static String clockTime(long seconds) {
        return String.format("%d:%02d", seconds / 3600, seconds / 60 % 60);
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
