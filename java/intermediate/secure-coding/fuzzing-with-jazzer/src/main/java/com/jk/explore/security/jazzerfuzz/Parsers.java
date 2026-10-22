package com.jk.explore.security.jazzerfuzz;

/** The parsers of the store behind one public door each, so that a fuzz target can reach them. A refusal (ParseException) is the right answer to bad input and is not a finding. */
public final class Parsers {

    private Parsers() {
    }

    public static void orderLineCareless(String line) {
        try {
            OrderLine.parseCareless(line);
        } catch (ParseException e) {
            // refused: fine
        }
    }

    public static void orderLine(String line) {
        try {
            OrderLine.parse(line);
        } catch (ParseException e) {
            // refused: fine
        }
    }

    public static void cartCareless(String text) {
        try {
            CartParser.parseCareless(text);
        } catch (ParseException e) {
            // refused: fine
        }
    }

    public static void cart(String text) {
        try {
            CartParser.parse(text);
        } catch (ParseException e) {
            // refused: fine
        }
    }

    /** The product search: does any product name contain what the customer typed? The careless version lets the customer write a pattern. */
    public static boolean searchCareless(String query) {
        return "SHO-1042 running shoes".matches(".*" + query + ".*");
    }

    public static boolean search(String query) {
        return "SHO-1042 running shoes".contains(query);
    }
}
