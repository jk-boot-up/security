package com.jk.explore.security.failclosed;

/** A dependency that did not answer within its time budget. */
final class TimeoutException extends ServiceException {

    TimeoutException(String service, long budget) {
        super(service + " did not answer within " + budget + " ms");
    }
}
