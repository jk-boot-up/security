package com.jk.explore.security.cia;

import java.util.ArrayList;
import java.util.List;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. */
public class CiaDemo {

    static final User ANA = new User("ana", Role.FINANCE);
    static final User RAJ = new User("raj", Role.SUPPORT);
    static final User MALLORY = new User("mallory", Role.SUPPORT);

    static final OrderRecord MAYA = new OrderRecord("O-101", "Maya Rao", "Laptop", "12 Palm Street", 1200);
    static final OrderRecord TOM = new OrderRecord("O-102", "Tom Lee", "Headphones", "7 Lake Road", 80);

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
        act3Integrity(out);
        act4Availability(out);
        act5Bill(out);
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
        line(out, "  raj reads O-101: address=" + store.read(RAJ, "O-101").address() + "  <- LEAK");
        store.setAmount(MALLORY, "O-101", 12);
        line(out, "  mallory changes the amount of O-101 from $1200 to $12, silently");
        line(out, "  ana reads O-101: amount=$" + store.read(ANA, "O-101").amountDollars() + "  <- SILENT CHANGE");
        TickServer.Result result = new TickServer(SERVER_CAPACITY).serve(flood(), null);
        line(out, "  " + (FLOOD_SIZE + 1) + " checkout requests arrive in one tick; the server can do " + SERVER_CAPACITY);
        line(out, "  served " + result.totalServed() + " of " + (FLOOD_SIZE + 1)
                + "; maya's request was refused  <- OUTAGE");
    }

    private static void act2Confidentiality(StringBuilder out) {
        line(out, "TWO. Confidentiality: each role sees only what it needs.");
        SecureStore store = freshStore();
        line(out, "  raj reads O-101: " + store.read(RAJ, "O-101").describe());
        line(out, "  ana reads O-101: " + store.read(ANA, "O-101").describe());
        line(out, "  mallory tries to change an amount: " + denied(() -> store.setAmount(MALLORY, "O-101", 12)));
    }

    private static void act3Integrity(StringBuilder out) {
        line(out, "THREE. Integrity: a change that bypasses the application is caught.");
        SecureStore store = freshStore();
        SecureStore.Stored stored = store.storageForDemo().get("O-101");
        store.storageForDemo().put("O-101", new SecureStore.Stored(stored.order().withAmount(12), stored.seal()));
        line(out, "  mallory edits O-101 in storage: amount $1200 -> $12, seal left as it was");
        try {
            store.read(ANA, "O-101");
            line(out, "  ana reads O-101: shown");
        } catch (TamperedOrderException e) {
            line(out, "  ana reads O-101: refused, " + e.getMessage() + "  <- DETECTED");
        }
        line(out, "  ana reads O-102: " + store.read(ANA, "O-102").describe());
    }

    private static void act4Availability(StringBuilder out) {
        line(out, "FOUR. Availability: one client cannot use up the whole server.");
        TickServer.Result result = new TickServer(SERVER_CAPACITY).serve(flood(), new RateLimiter(PER_CLIENT_LIMIT));
        line(out, "  each client may make " + PER_CLIENT_LIMIT + " requests per tick");
        line(out, "  bot: " + result.served().get("bot") + " served, " + result.refused().get("bot") + " refused");
        line(out, "  maya: " + result.served().get("maya") + " served");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. The bill: strict controls get in the way of emergencies.");
        SecureStore store = freshStore();
        line(out, "  raj, from support, reads O-101: " + store.read(RAJ, "O-101").describe());
        OrderView emergency = store.breakGlass(RAJ, "O-101", "courier at the wrong door, customer on the phone");
        line(out, "  raj uses emergency access: address=" + emergency.address());
        line(out, "  audit log entries: " + store.auditLog().size());
        line(out, "  " + store.auditLog().get(0));
    }

    private static SecureStore freshStore() {
        SecureStore store = new SecureStore(new Seal("demo-only-secret-not-for-real-use"));
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

    private static String denied(Runnable action) {
        try {
            action.run();
            return "allowed";
        } catch (AccessDeniedException e) {
            return "denied, " + e.getMessage();
        }
    }
}
