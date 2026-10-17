package com.jk.explore.security.backups;

/** Recovery point objective and recovery time objective, worked out from a timeline in minutes since midnight. */
final class Objectives {

    /** Rpo: how much work was lost. Rto: how long until service was back. */
    record Outcome(long rpoMinutes, long rtoMinutes, long ordersLost) {
    }

    private Objectives() {
    }

    static Outcome outcome(long lastBackup, long failure, long detected, long restoreMinutes, long ordersPerHour) {
        long rpo = failure - lastBackup;
        long rto = detected + restoreMinutes - failure;
        return new Outcome(rpo, rto, rpo * ordersPerHour / 60);
    }

    static String clock(long minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}
