package com.jk.explore.security.ssrfspring;

/** The importer refused an address, with the reason it gives back to the caller. */
final class ImportRefused extends Exception {

    ImportRefused(String reason) {
        super(reason);
    }
}
