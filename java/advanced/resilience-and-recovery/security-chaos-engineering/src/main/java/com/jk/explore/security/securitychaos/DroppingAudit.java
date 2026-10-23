package com.jk.explore.security.securitychaos;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/** The first version: a small in-memory buffer and a sender thread; what cannot be sent or does not fit is dropped, and nobody is told. */
final class DroppingAudit implements AuditTrail {

    static final int BUFFER = 5;

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(BUFFER);
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicInteger dropped = new AtomicInteger();
    private final Http http;
    private final String sinkUrl;
    private final Thread sender;
    private volatile boolean open = true;

    DroppingAudit(Http http, String sinkUrl) {
        this.http = http;
        this.sinkUrl = sinkUrl;
        this.sender = new Thread(this::drain);
        sender.setDaemon(true);
        sender.start();
    }

    private void drain() {
        while (open) {
            try {
                String id = queue.take();
                if (http.post(sinkUrl + "/audit", id).isEmpty()) {
                    dropped.incrementAndGet();
                }
                inFlight.decrementAndGet();
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    @Override
    public boolean record(String id) {
        inFlight.incrementAndGet();
        if (!queue.offer(id)) {
            inFlight.decrementAndGet();
            dropped.incrementAndGet();
        }
        return true;
    }

    @Override
    public void flush() {
        long end = System.nanoTime() + 20_000_000_000L;
        while (inFlight.get() > 0 && System.nanoTime() < end) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    int dropped() {
        return dropped.get();
    }

    @Override
    public void close() {
        open = false;
        sender.interrupt();
    }
}
