package com.jk.explore.security.ir;

import java.util.ArrayList;
import java.util.List;

/** What the organisation did <em>before</em> the incident. Almost everything that goes well or badly later is decided here. */
public record Preparation(boolean runbook, boolean monitoringOnDatabaseReads, boolean logsShippedCentrally, int logRetentionDays, boolean forensicsReady,
                          boolean knownGoodImages, boolean testedBackups) {

    public static Preparation none() {
        return new Preparation(false, false, false, 1, false, false, false);
    }

    public static Preparation monitoringOnly() {
        return new Preparation(false, true, false, 1, false, false, false);
    }

    public static Preparation full() {
        return new Preparation(true, true, true, 30, true, true, true);
    }

    public List<String> missing() {
        List<String> m = new ArrayList<>();
        if (!runbook) {
            m.add("no written runbook and contact list");
        }
        if (!monitoringOnDatabaseReads) {
            m.add("no alert on unusual database reads");
        }
        if (!logsShippedCentrally || logRetentionDays < 7) {
            m.add("logs not kept centrally for at least 7 days");
        }
        if (!forensicsReady) {
            m.add("no way to snapshot memory and disk");
        }
        if (!knownGoodImages) {
            m.add("no known-good image to rebuild from");
        }
        if (!testedBackups) {
            m.add("backups never tested");
        }
        return m;
    }

    public int score() {
        return 6 - missing().size();
    }
}
