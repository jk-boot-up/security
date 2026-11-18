package com.jk.explore.security.ir;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** The six things, in a fixed order, and whether each one is in place. */
    public Map<String, Boolean> checklist() {
        Map<String, Boolean> c = new LinkedHashMap<>();
        c.put("runbook and contact list", runbook);
        c.put("alert on unusual database reads", monitoringOnDatabaseReads);
        c.put("logs kept centrally 7+ days",
                logsShippedCentrally && logRetentionDays >= 7);
        c.put("snapshot of memory and disk", forensicsReady);
        c.put("known-good image to rebuild", knownGoodImages);
        c.put("tested backups", testedBackups);
        return c;
    }

    public List<String> missing() {
        List<String> m = new ArrayList<>();
        checklist().forEach((thing, inPlace) -> {
            if (!inPlace) {
                m.add(thing);
            }
        });
        return m;
    }

    public int score() {
        return 6 - missing().size();
    }
}
