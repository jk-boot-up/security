package com.jk.explore.security.fuzzing;

/** What a fuzzer calls: one parser, given raw bytes. A harness is the small piece of code each parser needs. */
interface Harness {

    String name();

    void run(String input, Coverage coverage) throws ParseException;

    static Harness of(String name, Target target) {
        return new Harness() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void run(String input, Coverage coverage) throws ParseException {
                target.parse(input, coverage);
            }
        };
    }

    @FunctionalInterface
    interface Target {
        Object parse(String input, Coverage coverage) throws ParseException;
    }

    Harness CARELESS_ORDERS = of("order lines, careless", OrderLine::parseCareless);
    Harness ORDERS = of("order lines, fixed", OrderLine::parse);
    Harness CARELESS_CARTS = of("carts, careless", CartParser::parseCareless);
    Harness CARTS = of("carts, fixed", CartParser::parse);
}
