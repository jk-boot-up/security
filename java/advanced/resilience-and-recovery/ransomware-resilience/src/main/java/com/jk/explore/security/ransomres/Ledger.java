package com.jk.explore.security.ransomres;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/** A bank's ledger as text: the same bytes for the same number, on every run. */
final class Ledger {

    static final int ROWS_PER_SNAPSHOT = 400;

    private Ledger() {
    }

    /** One backup of the ledger: ROWS_PER_SNAPSHOT lines "txn,account,amount in cents,DR or CR". */
    static byte[] snapshot(int number) {
        return rows(1000L + number, ROWS_PER_SNAPSHOT, number).getBytes(StandardCharsets.UTF_8);
    }

    /** One day's file of the live ledger (a bit larger than a backup, 600 rows). */
    static byte[] dayFile(int day) {
        return rows(5000L + day, 600, day * 1000).getBytes(StandardCharsets.UTF_8);
    }

    static byte[] canary() {
        return rows(9999L, 300, 900000).getBytes(StandardCharsets.UTF_8);
    }

    /** The sum of all amounts of a ledger file (credits are positive, debits negative). */
    static long total(byte[] file) {
        long sum = 0;
        for (String line : new String(file, StandardCharsets.UTF_8).split("\n")) {
            String[] f = line.split(",");
            if (f.length == 4) {
                long cents = Long.parseLong(f[2]);
                sum += f[3].equals("CR") ? cents : -cents;
            }
        }
        return sum;
    }

    static int rowCount(byte[] file) {
        int rows = 0;
        for (String line : new String(file, StandardCharsets.UTF_8).split("\n")) {
            if (line.split(",").length == 4) {
                rows++;
            }
        }
        return rows;
    }

    private static String rows(long seed, int count, int firstTxn) {
        Random random = new Random(seed);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("TX").append(firstTxn + i).append(',')
                    .append("ACC").append(10000 + random.nextInt(900)).append(',')
                    .append(100 + random.nextInt(250000)).append(',')
                    .append(random.nextBoolean() ? "CR" : "DR").append('\n');
        }
        return sb.toString();
    }
}
