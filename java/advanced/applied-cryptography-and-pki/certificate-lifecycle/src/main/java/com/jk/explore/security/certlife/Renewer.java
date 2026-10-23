package com.jk.explore.security.certlife;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/** Renews the certificate of a service when two thirds of its life are over, asking step-ca for a new one for the same key. */
final class Renewer implements AutoCloseable {

    private final StepCa ca;
    private final Service service;
    private final Thread thread;
    private final AtomicInteger renewed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private volatile boolean running = true;
    private int generation;
    private String current;

    Renewer(StepCa ca, Service service, String firstBase) {
        this.ca = ca;
        this.service = service;
        this.current = firstBase;
        this.thread = Thread.ofPlatform().daemon().start(this::loop);
    }

    /** When a certificate with this life should be renewed: after two thirds of it. */
    static Instant renewAt(Instant notBefore, Instant notAfter) {
        long life = Duration.between(notBefore, notAfter).toMillis();
        return notBefore.plusMillis(life * 2 / 3);
    }

    int renewed() {
        return renewed.get();
    }

    int failed() {
        return failed.get();
    }

    private void loop() {
        while (running) {
            try {
                Credential now = service.credential();
                Instant due = renewAt(now.leaf().getNotBefore().toInstant(), now.leaf().getNotAfter().toInstant());
                if (!Instant.now().isBefore(due)) {
                    String next = "renewed-" + (++generation);
                    try {
                        service.install(ca.renew(current, next));
                        current = next;
                        renewed.incrementAndGet();
                    } catch (java.io.IOException | IllegalStateException | java.security.GeneralSecurityException e) {
                        failed.incrementAndGet();
                    }
                }
                Thread.sleep(250);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    @Override
    public void close() throws InterruptedException {
        running = false;
        thread.interrupt();
        thread.join();
    }
}
