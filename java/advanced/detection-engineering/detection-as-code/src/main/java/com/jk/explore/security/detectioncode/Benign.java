package com.jk.explore.security.detectioncode;

import static com.jk.explore.security.detectioncode.Corpus.m;

import java.util.List;
import java.util.Map;
import java.util.Random;

/** Ordinary traffic of a bank for a week: logins, process starts on hosts, web requests. */
final class Benign {

    private static final String JAVA = "/usr/lib/jvm/java-21/bin/java";
    private static final String WIN = "C:\\Windows\\System32\\";

    /** host kind, parent, image, command line, user ($u is a staff member). */
    private static final String[][] PROCESSES = {
        {"web", "/usr/sbin/nginx", "/usr/sbin/nginx", "nginx: worker process", "www-data"},
        {"web", "/usr/sbin/nginx", "/usr/sbin/nginx", "nginx: cache manager process", "www-data"},
        {"app", "/usr/bin/systemd", JAVA, "java -jar /opt/bank/app.jar --spring.profiles.active=prod", "tomcat"},
        {"app", JAVA, "/usr/bin/openssl", "openssl x509 -noout -enddate -in /etc/bank/tls.crt", "tomcat"},
        {"any", "/usr/sbin/sshd", "/bin/bash", "-bash", "$u"},
        {"any", "/bin/bash", "/usr/bin/ls", "ls -la /opt/bank", "$u"},
        {"any", "/bin/bash", "/usr/bin/grep", "grep -r timeout /var/log/bank/app.log", "$u"},
        {"any", "/bin/bash", "/usr/bin/ps", "ps -ef", "$u"},
        {"any", "/bin/bash", "/usr/bin/systemctl", "systemctl status bank-app", "$u"},
        {"any", "/bin/bash", "/usr/bin/df", "df -h", "$u"},
        {"any", "/bin/bash", "/usr/bin/vim", "vim /etc/bank/app.properties", "$u"},
        {"any", "/bin/bash", "/usr/bin/crontab", "crontab -l", "$u"},
        {"any", "/bin/bash", "/usr/bin/curl", "curl -s https://api.bank.internal/v1/status", "$u"},
        {"any", "/bin/bash", "/usr/bin/wget", "wget -q https://repo.bank.internal/pkgs/tool.tgz -O /tmp/tool.tgz", "$u"},
        {"any", "/bin/bash", "/usr/bin/rm", "rm /tmp/report-$n.csv", "$u"},
        {"any", "/bin/bash", "/usr/bin/tar", "tar czf /tmp/backup-$n.tgz /opt/bank/conf", "svc-backup"},
        {"any", "/usr/sbin/cron", "/usr/bin/curl", "curl -s http://localhost:8080/health", "svc-monitor"},
        {"any", "/usr/sbin/cron", "/usr/sbin/logrotate", "/usr/sbin/logrotate /etc/logrotate.conf", "root"},
        {"any", "/usr/sbin/logrotate", "/usr/bin/gzip", "gzip /var/log/bank/app.log.1", "root"},
        {"any", "/usr/sbin/cron", "/bin/sh", "/bin/sh -c /usr/lib/sysstat/debian-sa1 1 1", "root"},
        {"any", "/usr/bin/python3", "/usr/bin/curl", "curl -sSf https://repo.bank.internal/pkgs/index.json -o /tmp/index.json", "svc-provision"},
        {"any", "/usr/bin/python3", "/usr/sbin/useradd", "useradd -m -G developers new$n", "svc-provision"},
        {"win", WIN + "services.exe", WIN + "svchost.exe", "svchost.exe -k netsvcs", "SYSTEM"},
        {"win", WIN + "cmd.exe", WIN + "schtasks.exe", "schtasks /query /fo LIST", "svc-deploy"},
        {"win", WIN + "cmd.exe", WIN + "wevtutil.exe", "wevtutil qe Security /c:5", "svc-monitor"},
        {"win", WIN + "explorer.exe", WIN + "cmd.exe", "cmd.exe /c dir C:\\bank", "$u"},
    };

    private static final String[] QUERIES = {"mortgage rate", "opening hours", "lost card", "wire transfer fees", "savings account", "overdraft limit",
        "pay a bill", "card limit", "branch near me", "interest on deposits", "statement copy"};
    private static final String[] TOWNS = {"Leeds", "York", "Bath", "Derby", "Luton", "Hull", "Poole", "Wells"};

    private Benign() {
    }

    static String staff(Random r) {
        return "emp" + (100 + r.nextInt(90));
    }

    static String host(Random r, String kind) {
        return switch (kind) {
            case "web" -> "web-0" + (1 + r.nextInt(4));
            case "app" -> "app-0" + (1 + r.nextInt(6));
            case "win" -> "win-0" + (1 + r.nextInt(3));
            default -> List.of("web-0" + (1 + r.nextInt(4)), "app-0" + (1 + r.nextInt(6)), "db-0" + (1 + r.nextInt(2)), "bast-01").get(r.nextInt(4));
        };
    }

    static String enc(String q) {
        return q.replace(" ", "%20").replace("'", "%27");
    }

    static Map<String, String> web(String host, String ip, String method, String path, String query, String status, String user) {
        Map<String, String> f = m("EventType", "web", "host", host, "src_ip", ip, "method", method, "uri", query.isEmpty() ? path : path + "?q=" + enc(query),
                "status", status, "User", user);
        if (!query.isEmpty()) {
            f.put("query", query);
        }
        return f;
    }

    static Map<String, String> login(Random r, String user, String ip, boolean ok) {
        return m("EventType", "login", "host", "auth-0" + (1 + r.nextInt(2)), "User", user, "src_ip", ip, "outcome", ok ? "success" : "failure");
    }

    static void events(Corpus.Sink s) {
        Random r = s.r;
        for (int day = 0; day < Corpus.DAYS; day++) {
            for (int i = 0; i < 900; i++) {
                boolean teller = r.nextInt(4) == 0;
                String user = teller ? "teller" + (10 + r.nextInt(120)) : r.nextInt(3) == 0 ? staff(r) : "cust" + (10_000 + r.nextInt(9_000));
                String ip = teller ? "10.30." + (1 + r.nextInt(12)) + ".1" : user.startsWith("emp") ? "10.20." + r.nextInt(8) + "." + (1 + r.nextInt(250)) : "100.64." + r.nextInt(200) + "." + (1 + r.nextInt(250));
                boolean ok = r.nextInt(1000) >= 25;
                long t = s.anyTime(day);
                s.benign(t, login(r, user, ip, ok));
                if (!ok && r.nextInt(10) == 0) {
                    s.benign(t + 20_000, login(r, user, ip, false));
                    s.benign(t + 45_000, login(r, user, ip, true));
                }
            }
            for (int i = 0; i < 1500; i++) {
                String[] p = PROCESSES[r.nextInt(PROCESSES.length)];
                String user = p[4].equals("$u") ? staff(r) : p[4];
                String cmd = p[3].replace("$n", String.valueOf(r.nextInt(900)));
                s.benign(s.anyTime(day), m("EventType", p[0].equals("win") ? "winproc" : "process_creation", "host", host(r, p[0]), "ParentImage", p[1], "Image", p[2], "CommandLine", cmd, "User", user));
            }
            for (int i = 0; i < 600; i++) {
                String ip = "100.64." + r.nextInt(200) + "." + (1 + r.nextInt(250));
                String user = "cust" + (10_000 + r.nextInt(9_000));
                String host = host(r, "web");
                int kind = r.nextInt(10);
                Map<String, String> f = switch (kind) {
                    case 0, 1, 2 -> web(host, ip, "GET", "/accounts/" + (1000 + r.nextInt(9000)) + "/balance", "", "200", user);
                    case 3, 4 -> web(host, ip, "POST", "/transfers", "", r.nextInt(30) == 0 ? "422" : "200", user);
                    case 5 -> web(host, ip, "GET", "/statements/" + (1000 + r.nextInt(9000)) + ".pdf", "", "200", user);
                    case 6, 7 -> web(host, ip, "GET", "/search/faq", QUERIES[r.nextInt(QUERIES.length)], "200", user);
                    case 8 -> web(host, ip, "GET", "/search/branches", TOWNS[r.nextInt(TOWNS.length)], "200", user);
                    default -> web(host, ip, "GET", r.nextInt(5) == 0 ? "/old/page" + r.nextInt(50) : "/login", "", r.nextInt(5) == 0 ? "404" : "200", user);
                };
                s.benign(s.anyTime(day), f);
            }
        }
    }
}
