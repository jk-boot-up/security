package com.jk.explore.security.jazzerfuzz;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class JazzerFuzzDemo {

    static final List<String> ORDER_SEEDS = List.of("SHO-1042,2,8990", "BAG-0007,1,4500");
    static final List<String> CART_SEEDS = List.of("[]", "[{\"sku\":\"SHO-1042\",\"qty\":2}]");
    static final List<String> CART_SEEDS_WITH_NOTE = List.of("[]", "[{\"sku\":\"SHO-1042\",\"qty\":2}]", "[{\"sku\":\"SHO-1042\",\"qty\":2,\"note\":\"hello\"}]");
    static final List<String> SEARCH_SEEDS = List.of("running", "SHO");
    static final int ORDER_RUNS = 300_000;
    static final int CART_RUNS = 1_000_000;

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        act1(out);
        act2(out);
        act3(out);
        act4(out);
        act5(out);
        return out.toString();
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }

    static JazzerRun.Result fuzz(String target, List<String> seeds, int runs, int maxLen) throws Exception {
        return JazzerRun.run(new JazzerRun.Settings(target, seeds, runs, 7, maxLen));
    }

    private static List<String> names(JazzerRun.Result result) {
        return result.findings().stream().map(Finding::describe).sorted().toList();
    }

    private static void act1(StringBuilder out) {
        line(out, "ONE. The example tests pass.");
        int passed = 0;
        String[][] examples = {{"SHO-1042,2,8990", "17980"}, {"BAG-0007,1,4500", "4500"}, {"SHO-1042,99,1", "99"}};
        for (String[] example : examples) {
            try {
                if (String.valueOf(OrderLine.parseCareless(example[0]).total().cents()).equals(example[1])) {
                    passed++;
                }
            } catch (ParseException e) {
                // would be a failed example
            }
        }
        for (String refused : new String[] {"SHO-1042,two,8990", "sho-1042,2,8990", ""}) {
            try {
                OrderLine.parseCareless(refused);
            } catch (ParseException e) {
                passed++;
            } catch (RuntimeException e) {
                // would be a failed example
            }
        }
        line(out, "  the order line parser passes " + passed + " of its " + (examples.length + 3) + " example tests");
        line(out, "  SHO-1042,2,8990 gives a total of " + safeTotal("SHO-1042,2,8990") + " cents, and SHO-1042,two,8990 is refused as invalid");
        line(out, "  but a line with only a product code, \"SHO-1042\": " + Crashes.signature(Parsers::orderLineCareless, "SHO-1042").replaceAll("@.*", "") + " in OrderLine.parseCareless  <- AN INPUT NOBODY WROTE A TEST FOR");
        line(out, "  a parser may refuse input; anything else it throws is a bug");
    }

    private static String safeTotal(String line) {
        try {
            return String.valueOf(OrderLine.parseCareless(line).total().cents());
        } catch (ParseException e) {
            return "refused";
        }
    }

    private static void act2(StringBuilder out) throws Exception {
        line(out, "TWO. The real Jazzer.");
        JazzerRun.Result result = fuzz("OrderLineCareless", ORDER_SEEDS, ORDER_RUNS, 4096);
        line(out, "  Jazzer 0.30 is started as a program on a fuzz target of three lines, with the two example inputs as seeds, random seed 7 and " + ORDER_RUNS + " runs");
        line(out, "  it adds branch counters to the parser's bytecode by itself and reports " + result.findings().size() + " distinct findings:");
        for (String name : names(result)) {
            line(out, "    " + name);
        }
        line(out, "  each is a bug the example tests missed, found without anyone guessing the input; the reports name the line, and the input is saved in a file");
    }

    private static void act3(StringBuilder out) throws Exception {
        line(out, "THREE. Guided by coverage, and by what you give it.");
        JazzerRun.Result without = fuzz("OrderLineCareless", List.of(), ORDER_RUNS, 4096);
        JazzerRun.Result with = fuzz("OrderLineCareless", ORDER_SEEDS, ORDER_RUNS, 4096);
        line(out, "  no seed inputs, " + ORDER_RUNS + " runs: coverage points reached " + without.coverage() + ", findings " + without.findings().size() + "  <- IT CANNOT GUESS A PRODUCT CODE");
        line(out, "  the two example lines as seeds: coverage points reached " + with.coverage() + ", findings " + with.findings().size());
        JazzerRun.Result plain = fuzz("CartCareless", CART_SEEDS, CART_RUNS, 4096);
        JazzerRun.Result noted = fuzz("CartCareless", CART_SEEDS_WITH_NOTE, CART_RUNS, 4096);
        line(out, "  the cart parser, " + CART_RUNS + " runs, seeds without a gift note: findings " + plain.findings().size());
        line(out, "  with one more seed that has a note field: " + names(noted));
        line(out, "  the seeds are the examples the team already has; the fuzzer mutates them, keeps what reaches new branches, and spends its runs near the interesting parts");
    }

    private static void act4(StringBuilder out) throws Exception {
        line(out, "FOUR. From crash to fix.");
        JazzerRun.Result found = fuzz("OrderLineCareless", ORDER_SEEDS, ORDER_RUNS, 4096);
        Consumer<String> careless = Parsers::orderLineCareless;
        List<String> minimal = new ArrayList<>();
        for (Finding f : found.findings().stream().sorted(java.util.Comparator.comparing(Finding::describe)).toList()) {
            String input = f.inputText();
            String smallest = Minimizer.minimize(careless, input);
            minimal.add(smallest);
            line(out, "  " + f.exception() + " in " + f.where().replaceAll(" \\(line.*", "") + ": the saved input is " + (input.length() > 0 ? "a random-looking line" : "empty") + ", made smallest: \"" + smallest + "\"; the fixed parser says " + refused(smallest));
        }
        line(out, "  each smallest input is now a regression test: it runs on every build, in milliseconds");
        JazzerRun.Result fixedOrder = fuzz("OrderLineFixed", ORDER_SEEDS, ORDER_RUNS, 4096);
        JazzerRun.Result fixedCart = fuzz("CartFixed", CART_SEEDS_WITH_NOTE, CART_RUNS, 4096);
        line(out, "  the fixed order parser under the same fuzzer: " + fixedOrder.findings().size() + " findings in " + fixedOrder.runs() + " runs; the fixed cart parser: " + fixedCart.findings().size() + " in " + fixedCart.runs());
    }

    private static String refused(String line) {
        try {
            OrderLine.parse(line);
            return "accepted";
        } catch (ParseException e) {
            return "refused (" + e.getMessage() + ")";
        }
    }

    private static void act5(StringBuilder out) throws Exception {
        line(out, "FIVE. What fuzzing finds besides crashes, and what it does not.");
        JazzerRun.Result search = fuzz("SearchBoxCareless", SEARCH_SEEDS, ORDER_RUNS, 4096);
        line(out, "  a search box that hands what the customer typed to the regular expression engine: " + names(search));
        JazzerRun.Result quoted = fuzz("SearchBoxFixed", SEARCH_SEEDS, ORDER_RUNS, 4096);
        line(out, "  the same box looking for the text as text: " + quoted.findings().size() + " findings  <- A SANITIZER REPORTS A KIND OF VULNERABILITY, NOT ONLY A CRASH");
        String cartNegative = "[{\"sku\":\"SHO-1042\",\"qty\":-5}]";
        String outcome;
        try {
            CartParser.parse(cartNegative);
            outcome = "parsed";
        } catch (ParseException e) {
            outcome = "refused";
        }
        line(out, "  a cart with a quantity of -5, " + cartNegative + ": " + outcome + "  <- NO CRASH, SO NO REPORT");
        line(out, "  whether -5 is allowed is a business rule; a fuzzer finds crashes, security issues its sanitizers know, and broken properties you wrote down, not wrong decisions");
        line(out, "  the cart's nesting crash: Jazzer's inputs stop at " + 4096 + " bytes by default, and the stack gave way at a depth of more than that; written by hand: " + deep());
        line(out, "  every parser needs its own fuzz target, a few lines each; in a build, a fixed budget of runs on every change, and the saved inputs as regression tests; longer runs at night, keeping the corpus");
    }

    private static String deep() {
        String nested = "[".repeat(100_000);
        String signature = Crashes.signature(Parsers::cartCareless, nested);
        return (signature == null ? "no crash" : signature.replaceAll("@.*", "")) + ", and the fixed parser says " + fixedDeep(nested);
    }

    private static String fixedDeep(String nested) {
        try {
            CartParser.parse(nested);
            return "accepted";
        } catch (ParseException e) {
            return "refused (" + e.getMessage() + ")";
        }
    }
}
