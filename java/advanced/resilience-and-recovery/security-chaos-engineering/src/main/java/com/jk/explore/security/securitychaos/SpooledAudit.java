package com.jk.explore.security.securitychaos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * The hardened version: the record is written to a journal file first, then sent. When the sink fails the records
 * wait, the service stops trying inline, and flush sends them in order once the sink is back. If the journal itself
 * cannot be written, record says no and the payment is refused.
 */
final class SpooledAudit implements AuditTrail {

    private final Http http;
    private final String sinkUrl;
    private final Path journal;
    private final List<String> pending = new ArrayList<>();
    private boolean sinkDown;

    SpooledAudit(Http http, String sinkUrl, Path journal) {
        this.http = http;
        this.sinkUrl = sinkUrl;
        this.journal = journal;
    }

    @Override
    public synchronized boolean record(String id) {
        try {
            Files.writeString(journal, id + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            return false;
        }
        pending.add(id);
        if (!sinkDown) {
            sendPending();
        }
        return true;
    }

    @Override
    public synchronized void flush() {
        sinkDown = false;
        sendPending();
    }

    private void sendPending() {
        while (!pending.isEmpty()) {
            if (http.post(sinkUrl + "/audit", pending.getFirst()).isEmpty()) {
                sinkDown = true;
                return;
            }
            pending.removeFirst();
        }
    }

    synchronized int waiting() {
        return pending.size();
    }

    @Override
    public void close() {
    }
}
