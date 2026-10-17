package com.jk.explore.security.failclosed;

/** A dependency that failed: it threw, or it did not answer in time. */
class ServiceException extends Exception {

    ServiceException(String message) {
        super(message);
    }
}
