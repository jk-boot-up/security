package com.jk.explore.security.siembasics;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/** One day of log lines from three sources in three formats, always the same: a web application (JSON), a database audit log (key=value, local time) and a host (syslog). */
public final class Sources {

    public static final Instant DAY = Instant.parse("2026-10-23T00:00:00Z");
    static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;
    static final DateTimeFormatter DB = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xx").withZone(ZoneOffset.ofHoursMinutes(5, 30));
    static final DateTimeFormatter SYSLOG = DateTimeFormatter.ofPattern("MMM d HH:mm:ss", java.util.Locale.ENGLISH).withZone(ZoneOffset.UTC);

    public record Lines(List<String> web, List<String> db, List<String> host) {
    }

    private Sources() {
    }

    static String web(Instant at, String type, String user, String ip, boolean ok) {
        return "{\"ts\":\"" + ISO.format(at) + "\",\"type\":\"" + type + "\",\"user\":\"" + user + "\",\"ip\":\"" + ip + "\",\"ok\":" + ok + "}";
    }

    static String db(Instant at, String actor, String client, String stmt, long rows) {
        return "time=" + DB.format(at) + " db=ledger actor=" + actor + " client=" + client + " stmt=" + stmt + " rows=" + rows + " result=ok";
    }

    static String host(Instant at, String host, String user, String ip, boolean ok) {
        return SYSLOG.format(at) + " " + host + " sshd[" + (1000 + Math.abs(user.hashCode()) % 9000) + "]: " + (ok ? "Accepted" : "Failed") + " password for " + user + " from " + ip + " port 55123 ssh2";
    }

    public static Lines day(long seed) {
        RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
        List<String> web = new ArrayList<>();
        List<String> db = new ArrayList<>();
        List<String> host = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            Instant t = DAY.plusSeconds(8 * 3600 + rng.nextInt(10 * 3600));
            String user = "user" + rng.nextInt(60);
            String ip = "10.1." + rng.nextInt(8) + "." + rng.nextInt(250);
            web.add(web(t, "login", user, ip, true));
            if (rng.nextInt(100) < 15) {
                for (int k = 0; k < 1 + rng.nextInt(3); k++) {
                    web.add(web(t.minusSeconds(20L * (k + 1)), "login", user, ip, false));
                }
            }
            if (rng.nextInt(100) < 30) {
                web.add(web(t.plusSeconds(60), "transfer", user, ip, true));
            }
        }
        for (int i = 0; i < 300; i++) {
            db.add(db(DAY.plusSeconds(rng.nextInt(86_400)), "user" + rng.nextInt(60), "10.0.0." + rng.nextInt(50), "SELECT", 1 + rng.nextInt(20)));
        }
        db.add(db(DAY.plusSeconds(2 * 3600), "svc-report", "10.0.9.1", "SELECT", 600_000));
        for (int i = 0; i < 20; i++) {
            db.add("time=garbled db=ledger actor=? stmt=" + i);
        }
        for (int i = 0; i < 500; i++) {
            host.add(host(DAY.plusSeconds(rng.nextInt(86_400)), "wks-" + String.format("%03d", 1 + rng.nextInt(40)), "user" + rng.nextInt(60), "10.1." + rng.nextInt(8) + "." + rng.nextInt(250), rng.nextInt(100) < 92));
        }
        // the attack: eight failures on the web and six on ssh in three minutes against alice, then a success, then a bulk export from the database
        Instant start = DAY.plusSeconds(9 * 3600 + 12 * 60);
        for (int i = 0; i < 8; i++) {
            web.add(web(start.plusSeconds(i * 20L), "login", "alice", "203.0.113.5", false));
        }
        for (int i = 0; i < 6; i++) {
            host.add(host(start.plusSeconds(10 + i * 25L), "bastion-01", "alice", "203.0.113.5", false));
        }
        web.add(web(start.plusSeconds(8 * 60), "login", "alice", "203.0.113.5", true));
        db.add(db(start.plusSeconds(8 * 60 + 5 * 60), "alice", "10.0.0.8", "SELECT", 500_000));
        return new Lines(web, db, host);
    }
}
