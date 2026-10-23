package com.jk.explore.security.ransomres;

/**
 * The recovery time and the recovery point as arithmetic. Nothing here reads a clock: the time to restore is the number of steps
 * and bytes that a restore really took, put through a fixed model of a production size and speed.
 */
final class Recovery {

    /** What one production backup weighs (the demo's files are a few KiB; this is the size they stand for). */
    static final long MODELLED_BYTES_PER_BACKUP = 25L * 1024 * 1024 * 1024;
    /** Declaring the incident and getting the go-ahead. */
    static final long DECLARE_SECONDS = 15 * 60;
    /** Building a clean environment to restore into. */
    static final long PROVISION_SECONDS = 45 * 60;
    /** Each step of the runbook (a list, a download, a write, a hash, a comparison). */
    static final long SECONDS_PER_STEP = 3;
    static final long TARGET_RTO_SECONDS = 4 * 3600;
    /** The bank's ledger takes this many transactions an hour. */
    static final long TRANSACTIONS_PER_HOUR = 1800;

    private Recovery() {
    }

    /** The recovery time in seconds for a restore of that many backups, that many steps, over a link of that many MiB per second. */
    static long rtoSeconds(int backups, int steps, long mibPerSecond) {
        long transferSeconds = backups * (MODELLED_BYTES_PER_BACKUP / (1024 * 1024)) / mibPerSecond;
        return DECLARE_SECONDS + PROVISION_SECONDS + steps * SECONDS_PER_STEP + transferSeconds;
    }

    /** Transactions between the last good backup and the moment the systems were cut off. */
    static long lostTransactions(int lastGoodBackupMinute, int cutOffMinute) {
        return (cutOffMinute - lastGoodBackupMinute) * TRANSACTIONS_PER_HOUR / 60;
    }

    /** The last backup at or before the minute of the infection, on a schedule of one backup every that many minutes from midnight. */
    static int lastGoodBackup(int infectionMinute, int everyMinutes) {
        return infectionMinute / everyMinutes * everyMinutes;
    }

    static String minutes(long seconds) {
        long tenths = (seconds * 10 + 30) / 60;
        return (tenths / 10) + "." + (tenths % 10) + " min";
    }

    static String clock(int minuteOfDay) {
        return String.format("%02d:%02d", minuteOfDay / 60, minuteOfDay % 60);
    }
}
