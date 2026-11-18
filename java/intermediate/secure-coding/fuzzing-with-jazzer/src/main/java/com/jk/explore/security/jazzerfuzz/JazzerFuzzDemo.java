package com.jk.explore.security.jazzerfuzz;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Runs the online store story in five numbered acts with the real Jazzer and prints exact, repeatable results. Each act prints
 * its input and its result in separate, labelled sections, and works out the values a beginner would wonder about.
 */
public class JazzerFuzzDemo {

    static final List<String> ORDER_SEEDS = List.of("SHO-1042,2,8990", "BAG-0007,1,4500");
    static final List<String> CART_SEEDS = List.of("[]", "[{\"sku\":\"SHO-1042\",\"qty\":2}]");
    static final List<String> CART_SEEDS_WITH_NOTE = List.of("[]", "[{\"sku\":\"SHO-1042\",\"qty\":2}]", "[{\"sku\":\"SHO-1042\",\"qty\":2,\"note\":\"hello\"}]");
    static final List<String> SEARCH_SEEDS = List.of("running", "SHO");
    static final int ORDER_RUNS = 300_000;
    static final int CART_RUNS = 1_000_000;
    static final int MAX_LEN = 4096;
    static final int RANDOM_SEED = 7;

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

    private static String pad(String text, int width) {
        return text + " ".repeat(Math.max(2, width - text.length()));
    }

    private static String quote(String text) {
        return "\"" + text + "\"";
    }

    static JazzerRun.Result fuzz(String target, List<String> seeds, int runs) throws Exception {
        return JazzerRun.run(new JazzerRun.Settings(target, seeds, runs, RANDOM_SEED, MAX_LEN));
    }

    private static List<Finding> sorted(JazzerRun.Result result) {
        return result.findings().stream().sorted(Comparator.comparing(Finding::describe)).toList();
    }

    /** A finding on two lines: the kind of exception, then where in the store's code. */
    private static void finding(StringBuilder out, String indent, Finding f) {
        line(out, indent + f.exception());
        line(out, indent + "  in " + f.where());
    }

    /** What the careless order parser makes of a line: a total, "refused (why)", or the crash and its line. */
    static String orderResult(String input) {
        try {
            return "total " + OrderLine.parseCareless(input).total().cents() + " cents";
        } catch (ParseException e) {
            return "refused (" + e.getMessage() + ")";
        } catch (RuntimeException e) {
            return crash(Parsers::orderLineCareless, input);
        }
    }

    /** "ArrayIndexOutOfBoundsException (line 24)", from the signature Crashes gives. */
    static String crash(Consumer<String> parser, String input) {
        String signature = Crashes.signature(parser, input);
        return signature.contains("@") ? signature.replaceAll("@.*:(\\d+)", " (line $1)") : signature;
    }

    private static String fixedOrder(String input) {
        try {
            OrderLine.parse(input);
            return "accepted";
        } catch (ParseException e) {
            return "refused (" + e.getMessage() + ")";
        }
    }

    /** How many random ASCII strings of eight characters there are for each one that is a product code: 128^8 / (26^3 x 1 x 10^4). */
    static BigInteger oneProductCodeIn() {
        BigInteger all = BigInteger.valueOf(128).pow(8);
        BigInteger codes = BigInteger.valueOf(26).pow(3).multiply(BigInteger.valueOf(10).pow(4));
        return all.divide(codes);
    }

    private static void act1(StringBuilder out) {
        line(out, "ONE. The example tests pass.");
        String[][] examples = {{"SHO-1042,2,8990", "total 17980 cents"}, {"BAG-0007,1,4500", "total 4500 cents"},
            {"SHO-1042,99,1", "total 99 cents"}, {"SHO-1042,two,8990", "refused (not a number)"},
            {"sho-1042,2,8990", "refused (bad product code)"}, {"", "refused (bad product code)"}};
        int passed = 0;
        for (String[] example : examples) {
            passed += orderResult(example[0]).equals(example[1]) ? 1 : 0;
        }
        line(out, "  THE EXAMPLE TESTS, " + passed + " OF " + examples.length + " PASS:");
        for (String[] example : examples) {
            line(out, "    " + pad(quote(example[0]), 22) + orderResult(example[0]));
        }
        String[] fields = ORDER_SEEDS.get(0).split(",");
        line(out, "  HOW THE TOTAL IS MADE (" + quote(ORDER_SEEDS.get(0)) + "):");
        line(out, "    fields    " + String.join(" | ", fields) + "  (code | quantity | cents)");
        line(out, "    total     " + fields[1] + " x " + fields[2] + " = " + orderResult(ORDER_SEEDS.get(0)).substring(6));
        line(out, "  THE INPUT NOBODY WROTE A TEST FOR:");
        line(out, "    " + quote("SHO-1042"));
        line(out, "  THE RESULT:");
        line(out, "    crash     " + orderResult("SHO-1042") + "  <- A BUG");
        line(out, "  THE RULE:");
        line(out, "    a parser may refuse input; anything else it throws is a bug");
    }

    private static void act2(StringBuilder out) throws Exception {
        line(out, "TWO. The real Jazzer.");
        JazzerRun.Result result = fuzz("OrderLineCareless", ORDER_SEEDS, ORDER_RUNS);
        line(out, "  THE RUN:");
        line(out, "    tool      Jazzer 0.30, started as a program of its own");
        line(out, "    target    OrderLineCareless, three lines");
        line(out, "    seeds     " + ORDER_SEEDS.stream().map(JazzerFuzzDemo::quote).collect(Collectors.joining(", ")));
        line(out, "    runs      " + ORDER_RUNS + ", random seed " + RANDOM_SEED + ", inputs up to " + MAX_LEN + " bytes");
        line(out, "    coverage  counters Jazzer adds to the bytecode by itself");
        line(out, "  THE FINDINGS, " + result.findings().size() + " DISTINCT:");
        for (Finding f : sorted(result)) {
            finding(out, "    ", f);
        }
        line(out, "  WHICH LINE IS WHICH (two lines written by hand):");
        for (String input : List.of("SHO-1042", "SHO-1042,2")) {
            int n = input.split(",").length;
            String at = crash(Parsers::orderLineCareless, input).replaceAll(".*\\((line \\d+)\\)", "$1");
            line(out, "    " + pad(quote(input), 14) + n + (n == 1 ? " field,  " : " fields, ") + at + " reads fields[" + n + "]");
        }
        line(out, "  HOW A FINDING IS NAMED:");
        line(out, "    kind      the \"== Java Exception\" line of the report");
        line(out, "    place     the first frame in the store's code");
        line(out, "    repeats   one DEDUP_TOKEN for each distinct stack");
        line(out, "    input     saved in a crash- file");
    }

    private static void act3(StringBuilder out) throws Exception {
        line(out, "THREE. Guided by coverage, and by what you give it.");
        JazzerRun.Result without = fuzz("OrderLineCareless", List.of(), ORDER_RUNS);
        JazzerRun.Result with = fuzz("OrderLineCareless", ORDER_SEEDS, ORDER_RUNS);
        line(out, "  NO SEED INPUTS, " + ORDER_RUNS + " RUNS:");
        line(out, "    coverage  " + without.coverage() + " points");
        line(out, "    findings  " + without.findings().size() + "  <- IT CANNOT GUESS A PRODUCT CODE");
        line(out, "  HOW LIKELY A RANDOM PRODUCT CODE IS (8 ASCII CHARACTERS):");
        line(out, "    letters   3 capitals, 26 of 128 each: (26/128)^3");
        line(out, "    dash      1 of 128");
        line(out, "    digits    4 digits, 10 of 128 each: (10/128)^4");
        line(out, "    together  128^8 / (26^3 x 10^4) = 1 in " + oneProductCodeIn());
        line(out, "  THE TWO EXAMPLE LINES AS SEEDS, " + ORDER_RUNS + " RUNS:");
        line(out, "    coverage  " + with.coverage() + " points");
        line(out, "    findings  " + with.findings().size());
        JazzerRun.Result plain = fuzz("CartCareless", CART_SEEDS, CART_RUNS);
        JazzerRun.Result noted = fuzz("CartCareless", CART_SEEDS_WITH_NOTE, CART_RUNS);
        line(out, "  THE CART PARSER, SEEDS WITHOUT A NOTE, " + CART_RUNS + " RUNS:");
        line(out, "    seeds     " + CART_SEEDS.stream().map(JazzerFuzzDemo::quote).collect(Collectors.joining(", ")));
        line(out, "    findings  " + plain.findings().size());
        line(out, "  WITH ONE MORE SEED THAT HAS A NOTE FIELD:");
        line(out, "    seed      " + quote(CART_SEEDS_WITH_NOTE.getLast()));
        for (Finding f : sorted(noted)) {
            line(out, "    finding   " + f.exception());
            line(out, "              in " + f.where());
        }
    }

    private static void act4(StringBuilder out) throws Exception {
        line(out, "FOUR. From crash to fix.");
        JazzerRun.Result found = fuzz("OrderLineCareless", ORDER_SEEDS, ORDER_RUNS);
        Consumer<String> careless = Parsers::orderLineCareless;
        String example = "SHO-1042,28990";
        List<String> steps = new ArrayList<>();
        Minimizer.minimize(careless, example, steps);
        line(out, "  HOW THE MINIMIZER WORKS (" + quote(example) + "):");
        line(out, "    " + pad(quote(example), 18) + crash(careless, example));
        for (String step : steps) {
            boolean shorter = step.length() < example.length();
            line(out, "    " + pad(quote(step), 18) + (shorter ? "shorter" : "plainer") + ", same crash");
            example = step;
        }
        List<String> smallest = new ArrayList<>();
        for (Finding f : sorted(found)) {
            smallest.add(Minimizer.minimize(careless, f.inputText()));
        }
        line(out, "  BEFORE THE FIX (each saved input, made smallest):");
        for (String s : smallest) {
            line(out, "    " + pad(quote(s), 25) + crash(careless, s));
        }
        String overflow = smallest.getLast();
        String[] f = overflow.split(",");
        long exact = Long.parseLong(f[1]) * Long.parseLong(f[2]);
        long wraps = Math.floorDiv(exact, 1L << 32);
        long low = exact - wraps * (1L << 32);
        line(out, "  HOW THE OVERFLOW IS MADE (" + f[1] + " x " + f[2] + "):");
        line(out, "    exact        " + f[1] + " x " + f[2] + " = " + exact);
        line(out, "    low 32 bits  " + exact + " - " + wraps + " x " + (1L << 32) + " = " + low);
        line(out, "    as an int    " + low + " - " + (1L << 32) + " = " + (int) exact);
        line(out, "    Money        a total cannot be negative  <- THE CRASH");
        line(out, "  AFTER THE FIX (the same inputs, fixed parser):");
        for (String s : smallest) {
            line(out, "    " + pad(quote(s), 25) + fixedOrder(s));
        }
        line(out, "    each one is a regression test, run on every build in ms");
        JazzerRun.Result fixedOrder = fuzz("OrderLineFixed", ORDER_SEEDS, ORDER_RUNS);
        JazzerRun.Result fixedCart = fuzz("CartFixed", CART_SEEDS_WITH_NOTE, CART_RUNS);
        line(out, "  THE FIXED PARSERS UNDER THE SAME FUZZER:");
        line(out, "    order lines  " + fixedOrder.findings().size() + " findings in " + fixedOrder.runs() + " runs");
        line(out, "    carts        " + fixedCart.findings().size() + " findings in " + fixedCart.runs() + " runs");
    }

    private static void act5(StringBuilder out) throws Exception {
        line(out, "FIVE. What fuzzing finds besides crashes, and what it does not.");
        JazzerRun.Result search = fuzz("SearchBoxCareless", SEARCH_SEEDS, ORDER_RUNS);
        line(out, "  THE SEARCH BOX, CARELESS (the words become a pattern):");
        for (Finding f : sorted(search)) {
            line(out, "    finding   " + f.exception());
            line(out, "              in " + f.where());
        }
        line(out, "    \"(\"       " + searchOutcome(Parsers::searchCareless, "("));
        JazzerRun.Result quoted = fuzz("SearchBoxFixed", SEARCH_SEEDS, ORDER_RUNS);
        line(out, "  THE SEARCH BOX, FIXED (the words are only text):");
        line(out, "    findings  " + quoted.findings().size() + "  <- A SANITIZER FINDS A VULNERABILITY");
        line(out, "    \"(\"       " + searchOutcome(Parsers::search, "("));
        String cartNegative = "[{\"sku\":\"SHO-1042\",\"qty\":-5}]";
        String outcome;
        try {
            CartParser.parse(cartNegative);
            outcome = "parsed";
        } catch (ParseException e) {
            outcome = "refused (" + e.getMessage() + ")";
        }
        line(out, "  THE INPUT, A CART WITH A QUANTITY OF -5:");
        line(out, "    " + quote(cartNegative));
        line(out, "  THE RESULT, FIXED PARSER:");
        line(out, "    " + outcome + "  <- NO CRASH, SO NO REPORT");
        String nested = "[".repeat(100_000);
        line(out, "  THE NESTING CRASH, WRITTEN BY HAND:");
        line(out, "    limit     Jazzer's inputs stop at " + MAX_LEN + " bytes by default");
        line(out, "    careless  " + nested.length() + " x \"[\": " + crash(Parsers::cartCareless, nested));
        line(out, "    fixed     " + fixedCart(nested));
    }

    private static String searchOutcome(java.util.function.Predicate<String> search, String query) {
        try {
            return "found: " + search.test(query);
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName();
        }
    }

    private static String fixedCart(String text) {
        try {
            CartParser.parse(text);
            return "accepted";
        } catch (ParseException e) {
            return "refused (" + e.getMessage() + ")";
        }
    }
}
