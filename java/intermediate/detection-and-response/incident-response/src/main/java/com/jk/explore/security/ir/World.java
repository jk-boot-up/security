package com.jk.explore.security.ir;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The shop's systems during the incident, and the attacker's ground truth. The attacker got into {@code web01} through a flaw in the application at minute 0 (01:00),
 * read the database password from its configuration, left three ways back in (a cron job, an SSH key and a hidden admin account), and from minute 20 copies
 * the customer table from {@code db01} at 1,000 rows a minute. Time is counted in minutes from 01:00 on the first day.
 */
public final class World {

    public static final int CUSTOMERS = 50_000;
    public static final int EXFIL_START = 20;
    public static final int EXFIL_RATE = 1_000;
    public static final int THREE_DAYS = 3 * 24 * 60;

    final Preparation prep;
    int minute;
    final int detectedAt;

    boolean webIsolated;
    boolean webRebuilt;
    boolean cron = true;
    boolean sshKey = true;
    boolean adminBackdoor = true;
    boolean vulnerabilityPatched;
    boolean dbPasswordValid = true;
    boolean dbSessionAlive = true;

    boolean memoryCaptured;
    boolean diskCaptured;
    boolean logsCollected;
    boolean scoped;
    boolean reinfected;
    int outageStart = -1;
    int reconnectedAt = -1;
    int rowsWhenStopped = -1;
    int stoppedAt = -1;
    String returnedVia;
    int exfiltrated;
    final List<String> timeline = new ArrayList<>();
    final List<String> actions = new ArrayList<>();

    public World(Preparation prep) {
        this.prep = prep;
        this.detectedAt = prep.monitoringOnDatabaseReads() ? 30 : THREE_DAYS; // an alert, or a customer's complaint three days later
    }

    /** True while the attacker is still copying data. */
    boolean exfilActive() {
        return minute >= EXFIL_START && exfiltrated < CUSTOMERS
                && dbSessionAlive && !webIsolated;
    }

    /** Moves time forward, letting the attacker keep working while nothing stops them. */
    void advance(int minutes) {
        for (int i = 0; i < minutes; i++) {
            if (exfilActive()) {
                exfiltrated = Math.min(CUSTOMERS, exfiltrated + EXFIL_RATE);
            }
            minute++;
        }
    }

    /** Called after each containment action: remembers how much had been copied the first time the copying stopped. */
    void noteIfStopped() {
        if (rowsWhenStopped < 0 && !exfilActive()) {
            rowsWhenStopped = exfiltrated;
            stoppedAt = minute;
        }
    }

    /** Every way back in the attacker has after 01:40, and the action that closes it. */
    public static Map<String, String> waysBackIn() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("cron job on web01", "rebuild from the image");
        m.put("SSH key on web01", "rebuild from the image");
        m.put("hidden admin account svc-backup", "find in logs, then disable");
        m.put("unpatched flaw in the application", "patch the flaw");
        m.put("stolen database password", "rotate the password");
        return m;
    }

    /** What the attacker can still use to get back in. */
    public List<String> footholdsLeft() {
        List<String> left = new ArrayList<>();
        if (cron && !webRebuilt) {
            left.add("cron job on web01");
        }
        if (sshKey && !webRebuilt) {
            left.add("SSH key on web01");
        }
        if (adminBackdoor) {
            left.add("hidden admin account svc-backup");
        }
        if (!vulnerabilityPatched) {
            left.add("unpatched flaw in the application");
        }
        if (dbPasswordValid) {
            left.add("stolen database password");
        }
        return left;
    }

    /** The time of day only, for a timeline that stays on one day. */
    public String time() {
        return clock().substring(6);
    }

    public String clock() {
        return clockAt(minute);
    }

    /** Minute 0 is 01:00 on day 0. */
    public static String clockAt(int minute) {
        int total = 60 + minute;
        return String.format("day %d %02d:%02d", total / 1440, (total % 1440) / 60, total % 60);
    }
}
