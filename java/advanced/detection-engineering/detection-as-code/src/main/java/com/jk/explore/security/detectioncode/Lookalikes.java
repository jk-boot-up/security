package com.jk.explore.security.detectioncode;

import static com.jk.explore.security.detectioncode.Corpus.m;

import java.util.Map;
import java.util.Random;

/**
 * Benign events that look like attacks. A "filtered" one is a case the rule has a filter for; a "lookalike" one fires the rule, and is its
 * false positive, until someone writes a filter or accepts the cost.
 */
final class Lookalikes {

    private static final String JAVA = "/usr/lib/jvm/java-21/bin/java";

    private Lookalikes() {
    }

    private static void add(Corpus.Sink s, int day, String kind, String rule, int min, int max, java.util.function.IntFunction<Map<String, String>> make) {
        int n = min + s.r.nextInt(max - min + 1);
        for (int i = 0; i < n; i++) {
            s.add(s.anyTime(day), make.apply(i), kind, rule, "-", 0);
        }
    }

    private static Map<String, String> proc(Random r, String host, String parent, String image, String cmd, String user) {
        return m("EventType", "process_creation", "host", Benign.host(r, host), "ParentImage", parent, "Image", image, "CommandLine", cmd, "User", user);
    }

    static void events(Corpus.Sink s) {
        Random r = s.r;
        for (int day = 0; day < Corpus.DAYS; day++) {
            add(s, day, "filtered", "web-server-shell", 8, 14, i -> proc(r, "app", JAVA, "/bin/sh", "/bin/sh -c /opt/bank/bin/healthcheck --deep", "tomcat"));
            add(s, day, "lookalike", "web-server-shell", 0, 2, i -> proc(r, "app", JAVA, "/bin/sh", "/bin/sh -c /opt/bank/bin/rotate-statements.sh --month", "tomcat"));

            add(s, day, "filtered", "new-admin-user", 2, 4, i -> proc(r, "any", "/usr/bin/python3", "/usr/sbin/useradd", "useradd -m -G sudo ops" + r.nextInt(900), "svc-provision"));
            add(s, day, "lookalike", "new-admin-user", 0, 1, i -> proc(r, "any", "/bin/bash", "/usr/sbin/usermod", "usermod -aG sudo emp" + (100 + r.nextInt(90)), "helpdesk" + (1 + r.nextInt(3))));

            add(s, day, "filtered", "credential-dump-tools", 1, 3, i -> proc(r, "bast", "/bin/bash", "/usr/bin/grep", "grep -ril mimikatz /srv/shared/iocs", "sec-hunt"));
            add(s, day, "lookalike", "credential-dump-tools", 0, 1, i -> proc(r, "bast", "/bin/bash", "/usr/bin/less", "less /home/sec" + (1 + r.nextInt(4)) + "/notes/mimikatz-detection.md", "sec" + (1 + r.nextInt(4))));

            add(s, day, "filtered", "download-pipe-shell", 1, 3, i -> proc(r, "any", "/bin/bash", "/bin/sh", "sh -c 'curl -sSf https://repo.bank.internal/bootstrap.sh | sh'", "svc-provision"));
            add(s, day, "lookalike", "download-pipe-shell", 0, 1, i -> proc(r, "any", "/bin/bash", "/bin/sh", "sh -c 'curl -fsSL https://get.monitoring-vendor.example/install.sh | sh'", "ops" + (1 + r.nextInt(5))));

            add(s, day, "filtered", "log-clearing", 6, 12, i -> proc(r, "any", "/usr/sbin/logrotate", "/usr/bin/rm", "rm -f /var/log/bank/app.log." + (3 + r.nextInt(5)) + ".gz", "root"));
            add(s, day, "lookalike", "log-clearing", 1, 3, i -> proc(r, "app", "/usr/sbin/cron", "/usr/bin/rm", "rm -f /var/log/bank-batch/old-" + r.nextInt(90) + ".log", "svc-batch"));

            add(s, day, "filtered", "scheduled-task-persistence", 1, 1, i -> proc(r, "any", "/bin/bash", "/bin/sh",
                    "sh -c '(crontab -l; echo \"0 2 * * * /opt/backup/run.sh /tmp/stage\") | crontab -'", "svc-backup"));
            add(s, day, "lookalike", "scheduled-task-persistence", 0, 2, i -> m("EventType", "winproc", "host", Benign.host(r, "win"),
                    "ParentImage", "C:\\Windows\\System32\\cmd.exe", "Image", "C:\\Windows\\System32\\schtasks.exe",
                    "CommandLine", "schtasks /create /tn BankNightlyBatch" + r.nextInt(9) + " /tr C:\\bank\\batch.exe /sc daily", "User", "svc-deploy"));

            add(s, day, "filtered", "web-injection-probe", 2, 5, i -> Benign.web(Benign.host(r, "web"), "100.64." + r.nextInt(200) + "." + (1 + r.nextInt(250)), "GET",
                    "/search/branches", "credit union select branch " + r.nextInt(9), "200", "cust" + (10_000 + r.nextInt(9_000))));
            add(s, day, "lookalike", "web-injection-probe", 0, 2, i -> Benign.web(Benign.host(r, "web"), "10.20." + r.nextInt(8) + "." + (1 + r.nextInt(250)), "GET",
                    "/helpdesk/article", "SQL union select tutorial " + r.nextInt(9), "200", staff(r)));
        }
        // Monday morning: the tellers of one branch behind one address have just come back and type old passwords
        for (int i = 0; i < 12; i++) {
            s.add(s.at(0, 9 * 3600 + i * 18), Benign.login(r, "teller" + (10 + i), "10.30.5.1", false), "lookalike", "failed-login-burst", "-", 0);
        }
        // a service account whose password was changed and whose job keeps retrying every 8 seconds, on two days
        for (int day : new int[] {2, 5}) {
            int start = 13 * 3600 + r.nextInt(3600);
            for (int i = 0; i < 14; i++) {
                s.add(s.at(day, start + i * 8), Benign.login(r, "svc-statements", "10.20.4.15", false), "lookalike", "failed-login-burst", "-", 0);
            }
        }
    }

    private static String staff(Random r) {
        return Benign.staff(r);
    }
}
