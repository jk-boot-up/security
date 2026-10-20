package com.jk.explore.security.fuzzing;

import java.util.List;
import java.util.Map;

/** Runs the online store story in five numbered acts and prints exact, repeatable results. */
public class FuzzingDemo {

    static final List<String> ORDER_SEEDS = List.of("SHO-1042,2,8990", "BAG-0007,1,4500");
    static final List<String> CART_SEEDS = List.of("{\"sku\":\"SHO-1042\",\"qty\":2,\"size\":\"42\"}", "[{\"sku\":\"BAG-0007\",\"qty\":1}]");
    static final long SEED = 7;
    static final int ORDER_BUDGET = 20_000;
    static final int CART_BUDGET = 100_000;
    static final String GIFT_BUG = "StringIndexOutOfBoundsException in CartParser.giftMessage";
    static final String NESTING_BUG = "StackOverflowError in CartParser";

    public static void main(String[] args) {
        System.out.print(run());
    }

    /** The whole demo as text, so the tests can compare it with the golden file. */
    public static String run() {
        StringBuilder out = new StringBuilder();
        act1Examples(out);
        Fuzzer.Report orders = Fuzzer.run(Harness.CARELESS_ORDERS, ORDER_SEEDS, ORDER_BUDGET, SEED, false);
        Fuzzer.Report blindCarts = Fuzzer.run(Harness.CARELESS_CARTS, CART_SEEDS, CART_BUDGET, SEED, false);
        act2Blind(out, orders, blindCarts);
        Fuzzer.Report guidedCarts = Fuzzer.run(Harness.CARELESS_CARTS, CART_SEEDS, CART_BUDGET, SEED, true);
        act3Guided(out, blindCarts, guidedCarts);
        act4CrashToFix(out, orders, guidedCarts);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String outcome(Harness harness, String input) {
        Fuzzer.Outcome o = Fuzzer.execute(harness, input);
        if (o.failure() != null) {
            return "crash: " + Crash.signature(o.failure());
        }
        return o.parsed() ? "parsed" : "refused as invalid";
    }

    private static void act1Examples(StringBuilder out) {
        line(out, "ONE. The example tests pass.");
        Coverage none = new Coverage();
        int passed = 0;
        try {
            passed += OrderLine.parseCareless("SHO-1042,2,8990", none).total().cents() == 17980 ? 1 : 0;
            passed += OrderLine.parseCareless("BAG-0007,1,4500", none).total().cents() == 4500 ? 1 : 0;
            passed += CartParser.parseCareless(CART_SEEDS.get(0), none) instanceof Value.Obj ? 1 : 0;
            passed += CartParser.parseCareless(CART_SEEDS.get(1), none) instanceof Value.Items ? 1 : 0;
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
        passed += outcome(Harness.CARELESS_ORDERS, "SHO-1042,two,8990").equals("refused as invalid") ? 1 : 0;
        passed += outcome(Harness.CARELESS_CARTS, "{\"sku\":}").equals("refused as invalid") ? 1 : 0;
        line(out, "  the store's two parsers, for order import lines and for carts, pass all " + passed + " of their 6 example tests");
        line(out, "  SHO-1042,2,8990 gives a total of 17980 cents, and SHO-1042,two,8990 is refused as invalid");
        line(out, "  but a line with only a product code, \"SHO-1042\": " + outcome(Harness.CARELESS_ORDERS, "SHO-1042")
                + "  <- AN INPUT NOBODY WROTE A TEST FOR");
        line(out, "  a parser may refuse input; anything else it throws is a bug");
    }

    private static void act2Blind(StringBuilder out, Fuzzer.Report orders, Fuzzer.Report carts) {
        line(out, "TWO. A mutation fuzzer.");
        line(out, "  it flips bits, inserts, deletes and replaces characters, swaps numbers for boundary values and nests brackets deeply");
        line(out, "  random numbers from " + Fuzzer.ALGORITHM + " with seed " + SEED + ", so every run finds the same crashes in the same order");
        report(out, orders);
        report(out, carts);
        line(out, "  each crash is a bug the example tests missed, found without anyone guessing the input");
    }

    private static void report(StringBuilder out, Fuzzer.Report report) {
        line(out, "  " + report.harness() + ", " + report.runs() + " runs from " + "2 seed inputs: " + report.crashes().size() + (report.crashes().size() == 1 ? " distinct crash" : " distinct crashes"));
        for (Crash crash : report.crashes().values()) {
            line(out, "    run " + crash.run() + ": " + crash.signature() + ", input " + crash.shown());
        }
    }

    private static void act3Guided(StringBuilder out, Fuzzer.Report blind, Fuzzer.Report guided) {
        line(out, "THREE. Guided by coverage.");
        line(out, "  the gift-message bug needs a field named note and a value starting with #: four letters and one more, all right at once");
        line(out, "  blind, " + blind.runs() + " runs: branches reached " + blind.branches() + ", gift-message bug found: "
                + (blind.firstRun(GIFT_BUG) > 0));
        line(out, "  guided, keeping every input that reaches a new branch: branches reached " + guided.branches()
                + ", inputs kept " + guided.corpus());
        Crash gift = guided.crashes().get(GIFT_BUG);
        line(out, "  the guided fuzzer finds it at run " + gift.run() + ", input " + gift.shown()
                + "  <- ONE LETTER AT A TIME, EACH ONE A NEW BRANCH");
        line(out, "  the work is counted in runs, not seconds, so the comparison is the same on every machine");
    }

    private static void act4CrashToFix(StringBuilder out, Fuzzer.Report orders, Fuzzer.Report carts) {
        line(out, "FOUR. From crash to fix.");
        for (Map.Entry<String, Crash> e : orders.crashes().entrySet()) {
            minimised(out, Harness.CARELESS_ORDERS, Harness.ORDERS, e.getValue());
        }
        minimised(out, Harness.CARELESS_CARTS, Harness.CARTS, carts.crashes().get(GIFT_BUG));
        line(out, "  the nesting crash is kept as it is, because how deep is too deep depends on the stack: the fixed parser says "
                + refusal(Harness.CARTS, carts.crashes().get(NESTING_BUG).input()));
        line(out, "  each smallest input is now a regression test, so the bug cannot come back unnoticed");
        Fuzzer.Report fixedOrders = Fuzzer.run(Harness.ORDERS, ORDER_SEEDS, ORDER_BUDGET, SEED, true);
        Fuzzer.Report fixedCarts = Fuzzer.run(Harness.CARTS, CART_SEEDS, CART_BUDGET, SEED, true);
        line(out, "  the fixed parsers under the same fuzzer: " + fixedOrders.crashes().size() + " crashes in " + fixedOrders.runs()
                + " runs, and " + fixedCarts.crashes().size() + " in " + fixedCarts.runs());
        int checked = 0;
        int same = 0;
        for (String input : fixedCarts.parsed()) {
            try {
                Value first = CartParser.parse(input, new Coverage());
                Value again = CartParser.parse(Value.print(first), new Coverage());
                checked++;
                same += first.equals(again) ? 1 : 0;
            } catch (ParseException e) {
                checked++;
            }
        }
        line(out, "  a property check: parse, print and parse again gives the same cart for " + same + " of " + checked + " inputs the fuzzer found valid");
    }

    private static void minimised(StringBuilder out, Harness careless, Harness fixed, Crash crash) {
        String smallest = Minimizer.minimise(careless, crash.input());
        line(out, "  " + crash.signature() + ": " + crash.input().length() + " characters made smallest, "
                + Crash.show(smallest) + "; the fixed parser says " + refusal(fixed, smallest));
    }

    private static String refusal(Harness harness, String input) {
        try {
            harness.run(input, new Coverage());
            return "accepted";
        } catch (ParseException e) {
            return "refused (" + e.getMessage() + ")";
        }
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. What fuzzing does not find.");
        line(out, "  a cart with a quantity of -5, " + Crash.show("{\"sku\":\"SHO-1042\",\"qty\":-5}") + ": "
                + outcome(Harness.CARTS, "{\"sku\":\"SHO-1042\",\"qty\":-5}") + "  <- NO CRASH, SO NO REPORT");
        line(out, "  whether -5 is allowed is a business rule; a fuzzer finds crashes and broken properties, not wrong decisions");
        line(out, "  every parser needs its own harness: this store wrote 2, one for each");
        line(out, "  in a build, a fixed budget of runs on every change; longer runs at night, keeping the inputs that reached new branches");
        line(out, "  coverage-guided tools such as Jazzer add the branch counters to the bytecode by themselves: the -with-jazzer version");
    }
}
