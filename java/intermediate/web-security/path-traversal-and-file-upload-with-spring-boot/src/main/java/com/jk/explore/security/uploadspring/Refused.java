package com.jk.explore.security.uploadspring;

/** The request was refused. The reason is for the security log; the customer only sees "not found" or "not accepted". */
public class Refused extends RuntimeException {

    public Refused(String reason) {
        super(reason);
    }
}
