package com.jk.explore.security.failclosed;

/** What the customer is told when something goes wrong. */
final class ErrorPages {

    private ErrorPages() {
    }

    /** The exception as it came: internal host names, versions and card numbers included. */
    static String naive(Exception e) {
        StringBuilder text = new StringBuilder("Error: ").append(e.getClass().getName()).append(": ").append(e.getMessage());
        for (StackTraceElement frame : e.getStackTrace()) {
            text.append("\n  at ").append(frame);
            if (text.length() > 400) {
                break;
            }
        }
        return text.toString();
    }

    /** A generic message and a reference. The detail goes to the log, not to the customer. */
    static String safe(String reference) {
        return "The payment could not be completed. Please try again later, quoting reference " + reference + ".";
    }
}
