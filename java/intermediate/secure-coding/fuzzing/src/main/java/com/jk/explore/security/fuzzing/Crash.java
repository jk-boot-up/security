package com.jk.explore.security.fuzzing;

/** An unexpected failure: any throwable other than {@link ParseException}, with the run that found it and the input. */
record Crash(String signature, int run, String input) {

    /** A crash is named by its type and the parser method it happened in, so the same bug is counted once. */
    static String signature(Throwable failure) {
        String where = "?";
        for (StackTraceElement frame : failure.getStackTrace()) {
            String name = frame.getClassName();
            if (name.startsWith(Crash.class.getPackageName() + ".") && !name.equals(Coverage.class.getName())) {
                String cls = name.substring(name.lastIndexOf('.') + 1);
                // a stack overflow can stop in any method of the recursion, so it is named by the class alone
                where = failure instanceof StackOverflowError ? cls : cls + "." + frame.getMethodName();
                break;
            }
        }
        return failure.getClass().getSimpleName() + " in " + where;
    }

    /** The input as it can be printed: short inputs as they are, long ones summarised. */
    String shown() {
        return show(input);
    }

    static String show(String input) {
        if (input.length() <= 40) {
            return "\"" + input + "\"";
        }
        return input.length() + " characters starting \"" + input.substring(0, 12) + "\"";
    }
}
