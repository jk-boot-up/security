package com.jk.explore.security.mtls;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** A service that keeps calling the ledger on new connections while certificates are renewed and the server reloads. */
final class Traffic implements AutoCloseable {

    final AtomicInteger succeeded = new AtomicInteger();
    final AtomicInteger failed = new AtomicInteger();
    private final AtomicReference<Credential> credential;
    private final Thread thread;
    private volatile boolean running = true;

    Traffic(Credential first, List<X509Certificate> trusted, String url) {
        credential = new AtomicReference<>(first);
        thread = Thread.ofPlatform().start(() -> {
            try {
                while (running) {
                    Caller.Reply r = Caller.call(credential.get(), trusted, url, "POST");
                    (!r.refused() && r.status() == 200 ? succeeded : failed).incrementAndGet();
                    Thread.sleep(40);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (java.security.GeneralSecurityException e) {
                failed.incrementAndGet();
            }
        });
    }

    /** Switches to the renewed certificate. */
    void use(Credential next) {
        credential.set(next);
    }

    @Override
    public void close() throws InterruptedException {
        running = false;
        thread.join();
    }
}
