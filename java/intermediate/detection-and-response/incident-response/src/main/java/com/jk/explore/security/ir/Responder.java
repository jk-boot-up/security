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
        w.timeline.add(String.format("%s  %-20s%s", w.time(), action, effect));
        w.actions.add(action);
    }

    /** Waits until the alert or the complaint arrives. */
    public Responder detect() {
        int wait = w.detectedAt - w.minute;
        if (wait > 0) {
            w.advance(wait);
        }
        log("detect", w.prep.monitoringOnDatabaseReads() ? "alert: unusual database reads" : "a customer says their data is sold");
        return this;
    }

    /** Cuts web01 off the network without turning it off, so that its memory is kept; this also cuts the attacker's route to db01. */
    public Responder isolateWeb01() {
        w.advance(5);
        w.webIsolated = true;
        w.outageStart = w.outageStart < 0 ? w.minute : w.outageStart;
        w.noteIfStopped();
        log("isolate web01", "memory kept, route to db01 cut");
        return this;
    }

    public Responder killDatabaseSessions() {
        w.advance(2);
        w.dbSessionAlive = false;
        w.noteIfStopped();
        log("kill db sessions", "attacker's db01 session ended");
        return this;
    }

    public Responder captureEvidence() {
        w.advance(15);
        if (!w.prep.forensicsReady()) {
            log("capture evidence", "no tooling: only the files left");
            w.diskCaptured = !w.webRebuilt;
            return this;
        }
        w.memoryCaptured = !w.webRebuilt;
        w.diskCaptured = !w.webRebuilt;
        w.logsCollected = w.prep.logsShippedCentrally();
        log("capture evidence", "memory, disk and logs, with hashes");
        return this;
    }

    public Responder scopeFromLogs() {
        w.advance(20);
        boolean covered = w.prep.logsShippedCentrally() && w.prep.logRetentionDays() * 24 * 60 >= w.detectedAt + 60;
        w.scoped = covered;
        log("scope from logs", covered ? "start 01:00; svc-backup made 01:40" : "logs are gone: extent unknown");
        return this;
    }

    public Responder rotateDatabasePassword() {
        w.advance(10);
        w.dbPasswordValid = false;
        log("rotate db password", "the stolen password stops working");
        return this;
    }

    public Responder disableBackdoorAccount() {
        w.advance(3);
        if (w.scoped) {
            w.adminBackdoor = false;
            log("disable svc-backup", "hidden admin account removed");
        } else {
            log("disable svc-backup", "not done: nobody knows it exists");
        }
        return this;
    }

    /** What panic does: it feels like cleaning up, and it destroys memory evidence while the cron job and the SSH key survive. */
    public Responder rebootWeb01() {
        w.advance(3);
        w.outageStart = w.outageStart < 0 ? w.minute : w.outageStart;
        boolean lost = !w.memoryCaptured;
        w.memoryCaptured = false;
        log("reboot web01",
                (lost ? "memory lost; " : "") + "cron job, SSH key stay");
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
        log("rebuild web01", w.prep.knownGoodImages() ? "from the image: cron job, key gone" : "by hand, no image: is it clean?");
        return this;
    }

    public Responder reconnect() {
        w.advance(10);
        w.webIsolated = false;
        w.reconnectedAt = w.minute;
        int open = w.footholdsLeft().size();
        log("reconnect web01", "back in service, " + open + " ways in open");
        return this;
    }

    /** Watches for two hours; an attacker who still has a way back in uses it. */
    public Responder monitorTwoHours() {
        w.advance(120);
        if (!w.footholdsLeft().isEmpty()) {
            w.reinfected = true;
            w.returnedVia = w.footholdsLeft().get(0);
            log("monitor 2 hours", "THE ATTACKER IS BACK");
        } else {
            log("monitor 2 hours", "quiet: nobody came back");
        }
        return this;
    }
}
