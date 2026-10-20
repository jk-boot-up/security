package com.jk.explore.security.fuzz;

/** Turns a crash into the source of a unit test, so that the bug stays fixed: the minimized input, the parser, and the one thing that may happen. */
public final class Regression {

    private Regression() {
    }

    public static String asJavaTest(Crash c, int number) {
        return "@Test\nvoid fuzzFinding" + number + "() {\n"
                + "    // " + c.signature() + ", found by the fuzzer on run " + c.foundAtExecution() + "\n"
                + "    assertCleanOutcome(\"" + escape(c.minimized()) + "\");\n}\n";
    }

    static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
