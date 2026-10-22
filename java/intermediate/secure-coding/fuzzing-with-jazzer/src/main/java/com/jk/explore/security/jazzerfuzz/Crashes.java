package com.jk.explore.security.jazzerfuzz;

import java.util.function.Consumer;

/** Runs a parser on an input and says what, if anything, it did wrong: the exception that is not a refusal, and the first line of the store's code in it. */
public final class Crashes {

    private Crashes() {
    }

    /** "ArrayIndexOutOfBoundsException@OrderLine.java:24", or null when the parser answered or refused. */
    public static String signature(Consumer<String> parser, String input) {
        try {
            parser.accept(input);
            return null;
        } catch (StackOverflowError e) {
            // the frame where the stack runs out depends on the compiler, so only the kind is a signature
            return "StackOverflowError";
        } catch (RuntimeException e) {
            for (StackTraceElement frame : e.getStackTrace()) {
                if (frame.getClassName().startsWith("com.jk.explore.security.jazzerfuzz.") && !frame.getClassName().contains(".targets.") && !frame.getClassName().endsWith("Parsers")) {
                    return e.getClass().getSimpleName() + "@" + frame.getFileName() + ":" + frame.getLineNumber();
                }
            }
            return e.getClass().getSimpleName();
        }
    }
}
