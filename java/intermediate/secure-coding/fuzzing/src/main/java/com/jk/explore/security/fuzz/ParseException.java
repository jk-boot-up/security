package com.jk.explore.security.fuzz;

/** The one failure a parser is allowed to report: "this input is not valid". Anything else that escapes is a bug. */
public class ParseException extends Exception {
    private static final long serialVersionUID = 1L;

    public ParseException(String message) {
        super(message);
    }
}
