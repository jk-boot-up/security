package com.jk.explore.security.threathunt;

import com.jk.explore.security.threathunt.Events.Dns;
import com.jk.explore.security.threathunt.Events.Login;
import com.jk.explore.security.threathunt.Events.Proc;
import com.jk.explore.security.threathunt.Events.Week;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/** A made-up but seeded week at a bank: 40 workstations, 5 servers, 60 users, a lot of ordinary activity, four attacks, and benign things that look like attacks. */
public final class Generator {

    public static final long DAY = 86_400;
    public static final long WEEK = 7 * DAY;
    static final List<String> POPULAR = List.of("news.example", "mail.example", "search.example", "maps.example", "video.example", "docs.example", "chat.example", "shop.example",
            "bank-intranet.example", "wiki.example", "cdn.example", "weather.example");
    static final List<String[]> PAIRS = List.of(new String[] {"explorer", "chrome"}, new String[] {"explorer", "outlook"}, new String[] {"services", "svchost"}, new String[] {"svchost", "updater"},
            new String[] {"outlook", "winword"}, new String[] {"explorer", "excel"}, new String[] {"chrome", "chrome"}, new String[] {"services", "backup-agent"}, new String[] {"explorer", "terminal"}, new String[] {"terminal", "git"});

    private Generator() {
    }

    public static String workstation(int i) {
        return String.format("wks-%03d", i);
    }

    public static Week week(long seed, boolean withAttacks) {
        RandomGenerator rng = RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
        List<Dns> dns = new ArrayList<>();
        List<Proc> procs = new ArrayList<>();
        List<Login> logins = new ArrayList<>();
        for (int h = 1; h <= 40; h++) {
            String host = workstation(h);
            for (long t = rng.nextInt(3600); t < WEEK; t += 3600) {
                dns.add(new Dns(host, "updates.vendor.example", t + rng.nextInt(30), 400));
            }
            for (long t = rng.nextInt(900); t < WEEK; t += 900) {
                dns.add(new Dns(host, "time.pool.example", t + rng.nextInt(5), 80));
            }
            for (int i = 0; i < 7 * 120; i++) {
                dns.add(new Dns(host, POPULAR.get(rng.nextInt(POPULAR.size())), rng.nextLong(WEEK), 500 + rng.nextInt(5000)));
            }
            for (int i = 0; i < 7 * 40; i++) {
                String[] pair = PAIRS.get(rng.nextInt(PAIRS.size()));
                procs.add(new Proc(host, pair[0], pair[1], rng.nextLong(WEEK)));
            }
        }
        for (int s = 1; s <= 5; s++) {
            String host = String.format("srv-%02d", s);
            for (long t = rng.nextInt(900); t < WEEK; t += 900) {
                dns.add(new Dns(host, "time.pool.example", t, 80));
            }
            for (int i = 0; i < 7 * 60; i++) {
                dns.add(new Dns(host, "cdn.example", rng.nextLong(WEEK), 2_000 + rng.nextInt(20_000)));
            }
        }
        // benign and periodic and rare: the nightly transfer to a partner
        for (int d = 0; d < 7; d++) {
            dns.add(new Dns("srv-02", "partner-batch.example", d * DAY + 2 * 3600 + rng.nextInt(60), 600_000_000L));
        }
        // benign, regular and rare: a monitoring heartbeat from a server to a partner's service every ten minutes
        for (long t = 100; t < WEEK; t += 600) {
            dns.add(new Dns("srv-02", "monitoring.partner.example", t, 200));
        }
        // benign and large: a one-off download of a patch set
        dns.add(new Dns("srv-04", "updates.vendor.example", 6 * DAY + 14 * 3600, 300_000_000L));
        // benign and rare: an administrator checking a network setting twice
        procs.add(new Proc("wks-007", "cmd", "ipconfig", 2 * DAY + 40_000));
        procs.add(new Proc("wks-007", "cmd", "ipconfig", 3 * DAY + 41_000));
        // logins: each user has a home address and works 8 to 18; five people work nights
        for (int u = 1; u <= 60; u++) {
            String user = "user" + u;
            boolean night = u > 55;
            for (int d = 0; d < 7; d++) {
                for (int k = 0; k < 1 + rng.nextInt(3); k++) {
                    int hour = night ? (22 + rng.nextInt(8)) % 24 : 8 + rng.nextInt(10);
                    logins.add(new Login(user, "10.1." + (u / 10) + "." + u, d * DAY + hour * 3600L + rng.nextInt(3600)));
                }
            }
        }
        logins.add(new Login("svc-backup", "10.2.0.9", 0));
        for (int d = 0; d < 7; d++) {
            logins.add(new Login("svc-backup", "10.2.0.9", d * DAY + 2 * 3600));
        }
        // benign and unusual: a manager logs in from a hotel address in business hours
        logins.add(new Login("user12", "10.1.1.12", 11 * 3600));
        logins.add(new Login("user12", "198.51.100.77", 4 * DAY + 11 * 3600));
        if (withAttacks) {
            // 1: command and control from a workstation, every 300 seconds with 10% jitter, from day 2 to day 6
            for (long t = 2 * DAY; t < 6 * DAY; t += 300 + rng.nextInt(61) - 30) {
                dns.add(new Dns("wks-014", "cdn-sync.example.invalid", t, 120));
            }
            // 2: a document starts a shell, which asks who the user is
            procs.add(new Proc("wks-014", "winword", "powershell", 2 * DAY - 600));
            procs.add(new Proc("wks-014", "powershell", "whoami", 2 * DAY - 590));
            // 3: three large uploads from a server to a new place on the fifth night
            for (int i = 0; i < 3; i++) {
                dns.add(new Dns("srv-03", "files-share.example.invalid", 5 * DAY + 3 * 3600 + i * 900, 800_000_000L));
            }
            // 4: the backup account is used at three in the morning from an address it has never used
            logins.add(new Login("svc-backup", "203.0.113.50", 5 * DAY + 3 * 3600 + 420));
        }
        return new Week(dns, procs, logins);
    }
}
