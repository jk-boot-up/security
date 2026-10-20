package com.jk.explore.security.ir;

/** Three ways the same incident can go, from what was prepared and what is done first. */
public final class Playbooks {

    private Playbooks() {
    }

    /** Nothing prepared, found by a customer three days later, and a panicked fix: reboot, change one password, go back online. */
    public static World panic() {
        World w = new World(Preparation.none());
        new Responder(w).detect().rebootWeb01().rotateDatabasePassword().reconnect().monitorTwoHours();
        return w;
    }

    /** Monitoring works and the alert is quick, but there is no plan: contain, rebuild by hand, and return to service. */
    public static World improvised() {
        World w = new World(Preparation.monitoringOnly());
        new Responder(w).detect().isolateWeb01().rebuildWeb01().reconnect().monitorTwoHours();
        return w;
    }

    /** The playbook: contain without destroying evidence, then capture, scope, close every way back in, rebuild from the image, reconnect, watch. */
    public static World playbook() {
        World w = new World(Preparation.full());
        new Responder(w).detect().isolateWeb01().killDatabaseSessions().captureEvidence().scopeFromLogs().rotateDatabasePassword().disableBackdoorAccount()
                .patchVulnerability().rebuildWeb01().reconnect().monitorTwoHours();
        return w;
    }
}
