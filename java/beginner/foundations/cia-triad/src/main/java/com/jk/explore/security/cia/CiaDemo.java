package com.jk.explore.security.cia;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Runs the online store story in seven numbered acts and prints exact, repeatable results. Each act
 * shows what goes in (the requests, or the order before a step) apart from what comes out.
 */
public class CiaDemo {

    static final User ANA = new User("ana", Role.FINANCE);
    static final User RAJ = new User("raj", Role.SUPPORT);
    static final User MALLORY = new User("mallory", Role.SUPPORT);

    static final OrderRecord MAYA = new OrderRecord("O-101", "Maya Rao", "Laptop", "12 Palm Street", 1200);
    static final OrderRecord TOM = new OrderRecord("O-102", "Tom Lee", "Headphones", "7 Lake Road", 80);

    /** The seal's secret key: a fixed demo value, so every run prints the same seals. */
    static final String SEAL_KEY = "demo-only-secret-not-for-real-use";

    static final int SERVER_CAPACITY = 10;
    static final int FLOOD_SIZE = 100;
    static final int PER_CLIENT_LIMIT = 3;

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1Naive(out);
        act2Confidentiality(out);
        act3Seal(out);
        act4SealSteps(out);
        act5Tamper(out);
        act6Availability(out);
        act7Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static void act1Naive(StringBuilder out) {
        line(out, "ONE. The first version, with no controls.");
        NaiveStore store = new NaiveStore();
        store.save(MAYA);
        store.save(TOM);
        line(out, "  THE REQUESTS:");
        line(out, "    " + who(RAJ) + " reads order O-101");
        line(out, "    " + who(MALLORY) + " changes the amount of O-101 to $12");
        line(out, "    flash sale: " + FLOOD_SIZE + " checkouts from a bot, 1 from maya");
        String address = store.read(RAJ, "O-101").address();
        store.setAmount(MALLORY, "O-101", 12);
        int amount = store.read(ANA, "O-101").amountDollars();
        TickServer.Result result = new TickServer(SERVER_CAPACITY).serve(flood(), null);
        line(out, "  THE RESULTS (nothing is checked):");
        line(out, "    raj gets the address: " + address + "  <- LEAK");
        line(out, "    ana sees amount=$" + amount + ", no one knows  <- SILENT CHANGE");
        line(out, "    the server can do " + SERVER_CAPACITY + ": all " + result.served().get("bot") + " went to the bot");
        line(out, "    maya's checkout: " + servedOrRefused(result, "maya") + "  <- OUTAGE");
    }

    private static void act2Confidentiality(StringBuilder out) {
        line(out, "TWO. Confidentiality: each role sees only what it needs.");
        SecureStore store = freshStore(new Seal(SEAL_KEY));
        line(out, "  THE REQUESTS:");
        line(out, "    " + who(RAJ) + " reads O-101");
        line(out, "    " + who(ANA) + " reads O-101");
        line(out, "    " + who(MALLORY) + " changes the amount of O-101 to $12");
        line(out, "  THE RESPONSES (filtered by role):");
        line(out, row("", "customer", "item", "address", "amount"));
        line(out, row(store.read(RAJ, "O-101"), "to raj"));
        line(out, row(store.read(ANA, "O-101"), "to ana"));
        line(out, "    to mallory " + denied(() -> store.setAmount(MALLORY, "O-101", 12)));
    }

    private static void act3Seal(StringBuilder out) {
        line(out, "THREE. Integrity: every order is saved with a seal.");
        SecureStore store = freshStore(new Seal(SEAL_KEY));
        String seal = store.storageForDemo().get("O-101").seal();
        line(out, "  BEFORE SEALING (the inputs):");
        line(out, "    order text  " + MAYA.sealedText());
        line(out, "    key         " + SEAL_KEY);
        line(out, "  AFTER SEALING (HMAC-SHA256 of the text, with the key):");
        line(out, "    seal        " + grouped(seal.substring(0, 32)));
        line(out, "                " + grouped(seal.substring(32)));
        line(out, "  the seal is " + seal.length() / 2 + " bytes, written as " + seal.length() + " hex characters");
        line(out, "  storage keeps the order and its seal side by side");
    }

    private static void act4SealSteps(StringBuilder out) {
        line(out, "FOUR. How the seal is made, step by step.");
        SealSteps.Steps s = SealSteps.of(SEAL_KEY, MAYA.sealedText());
        line(out, "  STEP 1. PAD THE KEY WITH ZERO BYTES TO 64 BYTES:");
        line(out, "    key         " + s.keyLength() + " bytes + " + (SealSteps.BLOCK - s.keyLength())
                + " zero bytes = " + s.paddedKey().length + " bytes");
        line(out, "    as hex      " + firstAndLast(s.paddedKey()));
        line(out, "  STEP 2. INNER HASH = SHA-256(key xor 36 + the text):");
        line(out, "    key xor 36  " + firstAndLast(s.innerKey()));
        line(out, "    inner       " + grouped(hex(s.inner()).substring(0, 32)));
        line(out, "                " + grouped(hex(s.inner()).substring(32)));
        line(out, "  STEP 3. SEAL = SHA-256(key xor 5c + inner hash):");
        line(out, "    key xor 5c  " + firstAndLast(s.outerKey()));
        line(out, "    seal        " + grouped(hex(s.seal()).substring(0, 32)));
        line(out, "                " + grouped(hex(s.seal()).substring(32)));
        line(out, "    the same seal Java's Mac gave in act three: "
                + hex(s.seal()).equals(new Seal(SEAL_KEY).of(MAYA)));
    }

    private static void act5Tamper(StringBuilder out) {
        line(out, "FIVE. A change behind the application is caught.");
        Seal sealer = new Seal(SEAL_KEY);
        SecureStore store = freshStore(sealer);
        SecureStore.Stored stored = store.storageForDemo().get("O-101");
        line(out, "  BEFORE THE EDIT (in storage):");
        line(out, "    order  " + stored.order().sealedText());
        line(out, "    seal   " + start(stored.seal()));
        OrderRecord edited = stored.order().withAmount(12);
        store.storageForDemo().put("O-101", new SecureStore.Stored(edited, stored.seal()));
        line(out, "  AFTER THE EDIT (mallory, directly in storage):");
        line(out, "    order  " + edited.sealedText());
        line(out, "    seal   " + start(stored.seal()) + "  (left as it was)");
        line(out, "  ANA READS O-101 (the seal is worked out again):");
        line(out, "    new seal  " + start(sealer.of(edited)));
        line(out, "    matches the stored seal: " + sealer.matches(edited, stored.seal()));
        try {
            store.read(ANA, "O-101");
            line(out, "    shown");
        } catch (TamperedOrderException e) {
            line(out, "    refused, " + e.getMessage() + "  <- DETECTED");
        }
        OrderView other = store.read(ANA, "O-102");
        line(out, "  ana reads O-102 (untouched): " + other.customer() + ", " + other.item() + ", $" + other.amountDollars());
    }

    private static void act6Availability(StringBuilder out) {
        line(out, "SIX. Availability: one client cannot use up the whole server.");
        line(out, "  THE REQUESTS (one tick):");
        line(out, "    " + FLOOD_SIZE + " checkouts from the bot, then 1 from maya");
        line(out, "    the server can do " + SERVER_CAPACITY + " requests in one tick");
        line(out, "  HOW THE LIMIT DECIDES (" + PER_CLIENT_LIMIT + " per client per tick):");
        RateLimiter trace = new RateLimiter(PER_CLIENT_LIMIT);
        for (Request request : flood()) {
            boolean allowed = trace.allow(request.client());
            int n = trace.count(request.client());
            if (n <= PER_CLIENT_LIMIT + 1) {
                line(out, String.format("    %-5s request %-3d count %d, %s: %s", request.client(), n, n,
                        n <= PER_CLIENT_LIMIT ? "at most " + PER_CLIENT_LIMIT : "over " + PER_CLIENT_LIMIT,
                        allowed ? "allowed" : "refused"));
            } else if (n == PER_CLIENT_LIMIT + 2) {
                line(out, String.format("    %-5s requests %d to %d: refused the same way", request.client(), n, FLOOD_SIZE));
            }
        }
        TickServer.Result without = new TickServer(SERVER_CAPACITY).serve(flood(), null);
        TickServer.Result with = new TickServer(SERVER_CAPACITY).serve(flood(), new RateLimiter(PER_CLIENT_LIMIT));
        line(out, "  THE RESULTS:");
        line(out, "    without the limit: bot " + without.served().get("bot") + " served, maya "
                + servedOrRefused(without, "maya"));
        line(out, "    with the limit:    bot " + with.served().get("bot") + " served, "
                + with.refused().get("bot") + " refused");
        line(out, "                       maya " + servedOrRefused(with, "maya") + "  <- SERVED");
    }

    private static void act7Bill(StringBuilder out) {
        line(out, "SEVEN. The bill: strict controls get in the way of emergencies.");
        SecureStore store = freshStore(new Seal(SEAL_KEY));
        line(out, "  THE REQUESTS (a courier is at the wrong door):");
        line(out, "    " + who(RAJ) + " reads O-101 as usual");
        line(out, "    raj asks for emergency access, with no reason");
        line(out, "    raj asks for emergency access, with a reason");
        line(out, "  THE RESPONSES:");
        line(out, "    as usual: address=" + shown(store.read(RAJ, "O-101").address()));
        line(out, "    no reason: " + denied(() -> store.breakGlass(RAJ, "O-101", " ")));
        OrderView emergency = store.breakGlass(RAJ, "O-101", "courier at the wrong door, customer on the phone");
        line(out, "    with a reason: address=" + emergency.address() + "  <- LOGGED");
        line(out, "  THE AUDIT LOG (" + store.auditLog().size() + " entry):");
        String entry = store.auditLog().get(0);
        int cut = entry.indexOf(": ") + 1;
        line(out, "    " + entry.substring(0, cut));
        line(out, "      " + entry.substring(cut + 1));
    }

    private static SecureStore freshStore(Seal sealer) {
        SecureStore store = new SecureStore(sealer);
        store.save(MAYA);
        store.save(TOM);
        return store;
    }

    /** The flood: a bot sends 100 checkout requests, then customer maya sends one. */
    static List<Request> flood() {
        List<Request> arrivals = new ArrayList<>();
        for (int i = 0; i < FLOOD_SIZE; i++) {
            arrivals.add(new Request("bot"));
        }
        arrivals.add(new Request("maya"));
        return arrivals;
    }

    private static String who(User user) {
        return user.name() + " (" + user.role().name().toLowerCase() + ")";
    }

    private static String row(OrderView v, String to) {
        return row(to, v.customer(), v.item(), shown(v.address()),
                v.amountDollars() == null ? shown(null) : "$" + v.amountDollars());
    }

    private static String row(String to, String customer, String item, String address, String amount) {
        return String.format("    %-9s%-10s%-8s%-16s%s", to, customer, item, address, amount).stripTrailing();
    }

    private static String shown(String field) {
        return field == null ? "[hidden]" : field;
    }

    private static String servedOrRefused(TickServer.Result result, String client) {
        int served = result.served().getOrDefault(client, 0);
        return served == 0 ? "refused" : served + " served";
    }

    private static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    /** "1a2b3c4d5e6f..." -> "1a2b3c4d 5e6f...", in groups of four bytes. */
    static String grouped(String hex) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 8) {
            out.append(i == 0 ? "" : " ").append(hex, i, Math.min(i + 8, hex.length()));
        }
        return out.toString();
    }

    /** The first eight hex characters and the next eight, then "...": enough to compare two seals. */
    private static String start(String hex) {
        return grouped(hex.substring(0, 16)) + " ...";
    }

    /** The first eight bytes and the last four of a 64-byte block. */
    private static String firstAndLast(byte[] block) {
        String h = hex(block);
        return grouped(h.substring(0, 16)) + " ... " + h.substring(h.length() - 8);
    }

    private static String denied(Runnable action) {
        try {
            action.run();
            return "allowed";
        } catch (AccessDeniedException e) {
            return "denied, " + e.getMessage();
        }
    }
}
