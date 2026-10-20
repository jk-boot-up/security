package com.jk.explore.security.ir;

/**
 * The actions an incident responder can take, each with a duration and a real effect on the {@link World}. The order matters, and the engine records what each
 * action did, so a response can be read as a timeline.
 */
public final class Responder {

    private final World w;

    public Responder(World world) {
        this.w = world;
    }

    private void log(String action, String effect) {
        w.timeline.add(String.format("%-14s %-26s %s", w.clock(), action, effect));
    }

    /** Waits until the alert or the complaint arrives. */
    public Responder detect() {
        int wait = w.detectedAt - w.minute;
        if (wait > 0) {
            w.advance(wait);
        }
        log("detect", w.prep.monitoringOnDatabaseReads() ? "alert: unusual database reads" : "a customer reports that their data is being sold");
        return this;
    }

    /** Cuts web01 off the network without turning it off, so that its memory is kept; this also cuts the attacker's route to db01. */
    public Responder isolateWeb01() {
        w.advance(5);
        w.webIsolated = true;
        w.outageStart = w.outageStart < 0 ? w.minute : w.outageStart;
        w.noteIfStopped();
        log("isolate web01", "network rule blocks it, memory kept; the route to db01 is cut");
        return this;
    }

    public Responder killDatabaseSessions() {
        w.advance(2);
        w.dbSessionAlive = false;
        w.noteIfStopped();
        log("kill database sessions", "the attacker's session on db01 is ended");
        return this;
    }

    public Responder captureEvidence() {
        w.advance(15);
        if (!w.prep.forensicsReady()) {
            log("capture evidence", "no tooling: only the files that were left");
            w.diskCaptured = !w.webRebuilt;
            return this;
        }
        w.memoryCaptured = !w.webRebuilt;
        w.diskCaptured = !w.webRebuilt;
        w.logsCollected = w.prep.logsShippedCentrally();
        log("capture evidence", "memory and disk of web01 snapshotted, logs collected with hashes");
        return this;
    }

    public Responder scopeFromLogs() {
        w.advance(20);
        boolean covered = w.prep.logsShippedCentrally() && w.prep.logRetentionDays() * 24 * 60 >= w.detectedAt + 60;
        w.scoped = covered;
        log("scope from logs", covered ? "start 01:00, web01 and db01 involved, svc-backup created at 01:40" : "logs from the first days are gone: start and extent unknown");
        return this;
    }

    public Responder rotateDatabasePassword() {
        w.advance(10);
        w.dbPasswordValid = false;
        log("rotate database password", "the stolen password no longer works");
        return this;
    }

    public Responder disableBackdoorAccount() {
        w.advance(3);
        if (w.scoped) {
            w.adminBackdoor = false;
            log("disable svc-backup", "the hidden admin account is removed");
        } else {
            log("disable svc-backup", "not done: nobody knows the account exists");
        }
        return this;
    }

    /** What panic does: it feels like cleaning up, and it destroys memory evidence while the cron job and the SSH key survive. */
    public Responder rebootWeb01() {
        w.advance(3);
        w.outageStart = w.outageStart < 0 ? w.minute : w.outageStart;
        boolean lost = !w.memoryCaptured;
        w.memoryCaptured = false;
        log("reboot web01", (lost ? "memory evidence lost; " : "") + "cron job and SSH key survive the reboot");
        return this;
    }

    public Responder patchVulnerability() {
        w.advance(30);
        w.vulnerabilityPatched = true;
        log("patch the flaw", "the way in is closed");
        return this;
    }

    public Responder rebuildWeb01() {
        w.advance(w.prep.knownGoodImages() ? 40 : 120);
        w.webRebuilt = true;
        log("rebuild web01", w.prep.knownGoodImages() ? "from the known-good image: cron job and SSH key are gone" : "by hand, with no image: slow, and nobody is sure it is clean");
        return this;
    }

    public Responder reconnect() {
        w.advance(10);
        w.webIsolated = false;
        w.reconnectedAt = w.minute;
        int open = w.footholdsLeft().size();
        log("reconnect web01", open == 0 ? "back in service" : "back in service with " + open + " way(s) back in still open");
        return this;
    }

    /** Watches for two hours; an attacker who still has a way back in uses it. */
    public Responder monitorTwoHours() {
        w.advance(120);
        if (!w.footholdsLeft().isEmpty()) {
            w.reinfected = true;
            log("monitor for 2 hours", "THE ATTACKER IS BACK, using the " + w.footholdsLeft().get(0));
        } else {
            log("monitor for 2 hours", "quiet");
        }
        return this;
    }
}
