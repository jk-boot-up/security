package com.jk.explore.security.fuzzing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Runs the online store story in five numbered acts and prints exact, repeatable results. Each act prints its input and its
 * result in separate, labelled sections, and works out the values a beginner would wonder about.
 */
public class FuzzingDemo {

    static final List<String> ORDER_SEEDS = List.of("SHO-1042,2,8990", "BAG-0007,1,4500");
    static final List<String> CART_SEEDS = List.of("{\"sku\":\"SHO-1042\",\"qty\":2,\"size\":\"42\"}", "[{\"sku\":\"BAG-0007\",\"qty\":1}]");
    static final long SEED = 7;
    static final int ORDER_BUDGET = 20_000;
    static final int CART_BUDGET = 100_000;
    static final String GIFT_BUG = "StringIndexOutOfBoundsException in CartParser.giftMessage";
    static final String NESTING_BUG = "StackOverflowError in CartParser";
    static final String MISSING_FIELD_BUG = "ArrayIndexOutOfBoundsException in OrderLine.parseCareless";
    static final String OVERFLOW_BUG = "IllegalArgumentException in Money.<init>";

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
        act4CrashToFix(out, orders, blindCarts, guidedCarts);
        act5Bill(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    private static String pad(String text, int width) {
        return text + " ".repeat(Math.max(2, width - text.length()));
    }

    /** A short form of an input for a table: long inputs keep their first characters and their length. */
    static String brief(String input) {
        if (input.length() > 32) {
            return "\"" + visible(input.substring(0, 6)) + "...\" " + input.length() + " chars";
        }
        return "\"" + visible(input) + "\"";
    }

    /** A bit flip can make an invisible control character; it is printed as \xNN so that it can be seen. */
    static String visible(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) {
            out.append(c < 32 || c == 127 ? String.format("\\x%02x", (int) c) : String.valueOf(c));
        }
        return out.toString();
    }

    /** What one parser makes of one input: a total, "parsed", "refused (why)" or "crash: signature". */
    private static String result(Harness harness, String input) {
        try {
            harness.run(input, new Coverage());
            if (harness == Harness.CARELESS_ORDERS || harness == Harness.ORDERS) {
                return "total " + OrderLine.parse(input, new Coverage()).total().cents() + " cents";
            }
            return "parsed";
        } catch (ParseException e) {
            return "refused (" + e.getMessage() + ")";
        } catch (RuntimeException | StackOverflowError bug) {
            return "crash: " + Crash.signature(bug);
        }
    }

    private static void act1Examples(StringBuilder out) {
        line(out, "ONE. The example tests pass.");
        record Example(String kind, Harness harness, String input, String expected) {
        }
        List<Example> examples = List.of(
                new Example("order", Harness.CARELESS_ORDERS, ORDER_SEEDS.get(0), "total 17980 cents"),
                new Example("order", Harness.CARELESS_ORDERS, ORDER_SEEDS.get(1), "total 4500 cents"),
                new Example("order", Harness.CARELESS_ORDERS, "SHO-1042,two,8990", "refused (not a number)"),
                new Example("cart", Harness.CARELESS_CARTS, CART_SEEDS.get(0), "parsed"),
                new Example("cart", Harness.CARELESS_CARTS, CART_SEEDS.get(1), "parsed"),
                new Example("cart", Harness.CARELESS_CARTS, "{\"sku\":}", "refused (not a value)"));
        long passed = examples.stream().filter(e -> result(e.harness(), e.input()).equals(e.expected())).count();
        line(out, "  THE EXAMPLE TESTS, " + passed + " OF " + examples.size() + " PASS:");
        for (Example e : examples) {
            line(out, "    " + pad(e.kind(), 6) + pad("\"" + e.input() + "\"", 30) + result(e.harness(), e.input()));
        }
        String[] fields = ORDER_SEEDS.get(0).split(",");
        line(out, "  HOW THE TOTAL IS MADE (\"" + ORDER_SEEDS.get(0) + "\"):");
        line(out, "    fields    " + String.join(" | ", fields) + "  (code | quantity | cents)");
        line(out, "    total     " + fields[1] + " x " + fields[2] + " = " + result(Harness.ORDERS, ORDER_SEEDS.get(0)).substring(6));
        String nobody = "SHO-1042";
        line(out, "  THE INPUT NOBODY WROTE A TEST FOR:");
        line(out, "    order \"" + nobody + "\"");
        line(out, "  THE RESULT:");
        line(out, "    fields    " + nobody.split(",").length + " field, but the code reads fields[1]");
        String[] crash = result(Harness.CARELESS_ORDERS, nobody).substring(7).split(" in ");
        line(out, "    crash     " + crash[0]);
        line(out, "    in        " + crash[1] + "  <- A BUG");
        line(out, "  THE RULE:");
        line(out, "    a parser may refuse input; anything else it throws is a bug");
    }

    private static void act2Blind(StringBuilder out, Fuzzer.Report orders, Fuzzer.Report carts) {
        line(out, "TWO. A mutation fuzzer.");
        line(out, "  THE MUTATIONS (1 or 2 per run):");
        line(out, "    flip      one bit of one character");
        line(out, "    insert    a random character");
        line(out, "    delete    one character");
        line(out, "    replace   one character by a random one");
        line(out, "    number    a number becomes " + String.join(" ", Fuzzer.BOUNDARIES));
        line(out, "    nest      " + Fuzzer.DEEP_NESTING + " opening brackets");
        line(out, "    random    " + Fuzzer.ALGORITHM + ", seed " + SEED + ": the same runs every time");
        line(out, "  THE FIRST RUNS (seeds " + ORDER_SEEDS.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) + "):");
        for (Fuzzer.Step step : orders.firstRuns()) {
            line(out, "    " + pad(String.valueOf(step.run()), 3) + pad(step.how(), 16) + pad(brief(step.input()), 25) + step.result());
        }
        crashes(out, "ORDER LINES", orders);
        Crash overflow = orders.crashes().get(OVERFLOW_BUG);
        String[] f = overflow.input().split(",");
        long exact = Long.parseLong(f[1]) * Long.parseLong(f[2]);
        long wraps = Math.floorDiv(exact, 1L << 32);
        long low = exact - wraps * (1L << 32);
        line(out, "  HOW THE OVERFLOW IS MADE (" + f[1] + " x " + f[2] + "):");
        line(out, "    exact        " + f[1] + " x " + f[2] + " = " + exact);
        line(out, "    low 32 bits  " + exact + " - " + wraps + " x " + (1L << 32) + " = " + low);
        line(out, "    as an int    " + low + " - " + (1L << 32) + " = " + (int) exact);
        line(out, "    Money        a total cannot be negative  <- THE CRASH");
        crashes(out, "CARTS", carts);
    }

    private static void crashes(StringBuilder out, String what, Fuzzer.Report report) {
        line(out, "  THE CRASHES, " + what + " (" + report.runs() + " runs, careless parser):");
        for (Crash crash : report.crashes().values()) {
            String[] where = crash.signature().split(" in ");
            line(out, "    " + pad("run " + crash.run(), 10) + where[0]);
            line(out, "    " + pad("", 10) + "in " + where[1]);
            line(out, "    " + pad("", 10) + brief(crash.input()));
        }
    }

    private static void act3Guided(StringBuilder out, Fuzzer.Report blind, Fuzzer.Report guided) {
        line(out, "THREE. Guided by coverage.");
        line(out, "  THE TARGET, A GIFT MESSAGE:");
        line(out, "    needs     a field named note, a value starting with #");
        line(out, "    blind     a seed changed at random, never kept");
        line(out, "  BLIND, " + blind.runs() + " RUNS:");
        line(out, "    branches  " + blind.branches());
        line(out, "    found     " + (blind.firstRun(GIFT_BUG) > 0 ? "yes" : "no"));
        line(out, "  GUIDED, THE INPUTS KEPT FOR A NEW BRANCH:");
        for (Fuzzer.Kept kept : guided.kept()) {
            String names = kept.newBranches().stream().map(CartParser::branchName).collect(Collectors.joining(", "));
            line(out, "    " + pad(String.valueOf(kept.run()), 6) + pad(brief(kept.input()), 34) + names);
        }
        Crash gift = guided.crashes().get(GIFT_BUG);
        line(out, "  GUIDED, " + guided.runs() + " RUNS:");
        line(out, "    branches  " + guided.branches() + ", inputs kept " + guided.corpus() + " (" + CART_SEEDS.size() + " seeds + "
                + guided.kept().size() + ")");
        line(out, "    found     at run " + gift.run() + "  <- ONE LETTER AT A TIME");
        line(out, "    input     " + brief(gift.input()));
        String note = gift.input().replaceAll(".*\"note\":\"([^\"]*)\".*", "$1");
        line(out, "  HOW THE CRASH IS MADE:");
        line(out, "    note               \"" + note + "\"");
        line(out, "    indexOf(':')       " + note.indexOf(':'));
        String[] crash = result(Harness.CARELESS_CARTS, gift.input()).substring(7).split(" in ");
        line(out, "    substring(1, " + note.indexOf(':') + ")   " + crash[0]);
        line(out, "    in                 " + crash[1]);
    }

    private static void act4CrashToFix(StringBuilder out, Fuzzer.Report orders, Fuzzer.Report blindCarts, Fuzzer.Report carts) {
        line(out, "FOUR. From crash to fix.");
        Crash gift = carts.crashes().get(GIFT_BUG);
        List<String> steps = new ArrayList<>();
        Minimizer.minimise(Harness.CARELESS_CARTS, gift.input(), steps);
        line(out, "  HOW DELTA DEBUGGING SHRINKS THE GIFT-MESSAGE CRASH:");
        line(out, "    " + pad(String.valueOf(gift.input().length()), 4) + brief(gift.input()));
        for (String step : steps) {
            line(out, "    " + pad(String.valueOf(step.length()), 4) + brief(step) + (step.equals(steps.getLast())
                    ? "  <- STILL CRASHES" : ""));
        }
        record Smallest(String input, Harness careless, Harness fixed) {
        }
        List<Smallest> smallest = List.of(
                new Smallest(Minimizer.minimise(Harness.CARELESS_ORDERS, orders.crashes().get(MISSING_FIELD_BUG).input()),
                        Harness.CARELESS_ORDERS, Harness.ORDERS),
                new Smallest(Minimizer.minimise(Harness.CARELESS_ORDERS, orders.crashes().get(OVERFLOW_BUG).input()),
                        Harness.CARELESS_ORDERS, Harness.ORDERS),
                new Smallest(steps.getLast(), Harness.CARELESS_CARTS, Harness.CARTS),
                new Smallest(blindCarts.crashes().get(NESTING_BUG).input(), Harness.CARELESS_CARTS, Harness.CARTS));
        line(out, "  BEFORE THE FIX (the smallest inputs, careless parsers):");
        for (Smallest s : smallest) {
            String crash = result(s.careless(), s.input()).substring(7);
            line(out, "    " + pad(brief(s.input()), 25) + crash.substring(0, crash.indexOf(' ')));
        }
        line(out, "  AFTER THE FIX (the same inputs, fixed parsers):");
        for (Smallest s : smallest) {
            line(out, "    " + pad(brief(s.input()), 25) + result(s.fixed(), s.input()));
        }
        line(out, "    each one is now a regression test (the nesting one as it is)");
        Fuzzer.Report fixedOrders = Fuzzer.run(Harness.ORDERS, ORDER_SEEDS, ORDER_BUDGET, SEED, true);
        Fuzzer.Report fixedCarts = Fuzzer.run(Harness.CARTS, CART_SEEDS, CART_BUDGET, SEED, true);
        line(out, "  THE FIXED PARSERS UNDER THE SAME FUZZER:");
        line(out, "    order lines  " + fixedOrders.crashes().size() + " crashes in " + fixedOrders.runs() + " runs");
        line(out, "    carts        " + fixedCarts.crashes().size() + " crashes in " + fixedCarts.runs() + " runs");
        int checked = 0;
        int same = 0;
        String shown = null;
        String printed = null;
        for (String input : fixedCarts.parsed()) {
            checked++;
            try {
                Value first = CartParser.parse(input, new Coverage());
                String text = Value.print(first);
                boolean equal = first.equals(CartParser.parse(text, new Coverage()));
                same += equal ? 1 : 0;
                if (shown == null && equal && !text.equals(input) && input.length() <= 32) {
                    shown = input;
                    printed = text;
                }
            } catch (ParseException e) {
                // counted as checked, not as the same
            }
        }
        line(out, "  A PROPERTY CHECK, PARSE, PRINT AND PARSE AGAIN:");
        line(out, "    input     " + brief(shown));
        line(out, "    printed   " + brief(printed));
        line(out, "    same cart for " + same + " of " + checked + " inputs the fuzzer found valid");
    }

    private static void act5Bill(StringBuilder out) {
        line(out, "FIVE. What fuzzing does not find.");
        String cart = "{\"sku\":\"SHO-1042\",\"qty\":-5}";
        line(out, "  THE INPUT, A CART WITH A QUANTITY OF -5:");
        line(out, "    " + brief(cart));
        line(out, "  THE RESULT, FIXED PARSER:");
        line(out, "    " + result(Harness.CARTS, cart) + "  <- NO CRASH, SO NO REPORT");
        line(out, "  WHAT A FUZZER CANNOT KNOW:");
        line(out, "    whether -5 is allowed is a business rule");
        line(out, "    it finds crashes and broken properties, not wrong decisions");
        line(out, "  WHAT IT COSTS:");
        line(out, "    harnesses  2, one for each parser");
        line(out, "    a build    a fixed budget of runs on every change");
        line(out, "    at night   longer runs, keeping the inputs that reached new code");
        line(out, "    Jazzer     adds the branch counters to the bytecode by itself");
    }
}
