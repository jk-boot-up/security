package com.jk.explore.security.fuzzing;

/** The one failure a parser is allowed to have: "this input is not valid". Anything else a parser throws is a bug. */
final class ParseException extends Exception {

    ParseException(String message) {
        super(message);
    }
}
