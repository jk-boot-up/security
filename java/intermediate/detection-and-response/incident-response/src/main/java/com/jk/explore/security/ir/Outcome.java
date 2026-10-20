package com.jk.explore.security.ir;

import java.util.List;

/** What a response achieved, in the terms of the post-incident review. {@code rowsCopiedBeforeStop} is -1 if the copying was never stopped. */
public record Outcome(String name, int foundAfterMinutes, int rowsCopiedBeforeStop, int rowsCopiedInTotal, List<String> footholdsLeft, boolean evidenceKept,
                      boolean rootCauseKnown, int outageMinutes, boolean attackerReturned) {

    public static Outcome of(String name, World w) {
        int outage = w.outageStart >= 0 && w.reconnectedAt >= 0 ? w.reconnectedAt - w.outageStart : 0;
        return new Outcome(name, w.detectedAt, w.rowsWhenStopped, w.exfiltrated, w.footholdsLeft(), w.memoryCaptured && w.diskCaptured && w.logsCollected, w.scoped, outage, w.reinfected);
    }

    public boolean recovered() {
        return footholdsLeft.isEmpty() && !attackerReturned;
    }
}
