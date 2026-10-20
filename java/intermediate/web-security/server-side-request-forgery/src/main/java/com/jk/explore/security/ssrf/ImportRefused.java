package com.jk.explore.security.ssrf;

/** The import was refused. The reason is for the security log; the customer is only told that the image could not be imported. */
public class ImportRefused extends RuntimeException {

    public ImportRefused(String reason) {
        super(reason);
    }
}
