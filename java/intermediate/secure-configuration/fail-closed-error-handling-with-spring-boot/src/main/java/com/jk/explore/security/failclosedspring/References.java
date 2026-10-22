package com.jk.explore.security.failclosedspring;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** Numbers a refusal so the customer can quote it and support can find the log line. */
@Component
class References {

    private final AtomicInteger next = new AtomicInteger(1);

    String next() {
        return "REF-%04d".formatted(next.getAndIncrement());
    }
}
