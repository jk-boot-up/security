package com.jk.explore.security.dast;

import java.util.List;

public class DastDemo {

    static final List<Integer> XSS_AND_SQL = List.of(40012, 40018);

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    static String found(List<Alert> alerts, boolean login) {
        List<Flaw> pool = Flaw.PLANTED.stream().filter(f -> login || !f.needsLogin()).toList();
        long hit = pool.stream().filter(f -> f.raisedIn(alerts)).count();
        return hit + " of " + pool.size();
    }

    static String line(String what, List<Alert> alerts) {
        long xss = Flaw.PLANTED.stream().filter(f -> f.alert().startsWith("Cross Site") && f.raisedIn(alerts)).count();
        long sql = Flaw.PLANTED.stream().filter(f -> f.alert().startsWith("SQL") && f.raisedIn(alerts)).count();
        long rest = Flaw.PLANTED.stream().filter(f -> !f.alert().startsWith("Cross Site") && !f.alert().startsWith("SQL") && f.raisedIn(alerts)).count();
        return String.format("%s: %d of 3 script flaws, %d of 1 query flaw, %d of 5 cookie and header flaws", what, xss, sql, rest);
    }

    static String names(List<Flaw> flaws) {
        return flaws.isEmpty() ? "none" : String.join(", ", flaws.stream().map(Flaw::id).toList());
    }

    static long byRisk(List<Alert> alerts, int risk) {
        return alerts.stream().filter(a -> a.risk() == risk).count();
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        try (BankApp app = new BankApp(false)) {
            String target = "http://host.docker.internal:" + app.port();
            out.append("--- the application under test\n");
            out.append("planted flaws: " + Flaw.PLANTED.size() + ", of them behind the login: " + Flaw.PLANTED.stream().filter(Flaw::needsLogin).count() + "\n");

            out.append("--- a passive scan: it only reads the responses\n");
            List<Alert> passive = Zap.scan(target, null, List.of());
            out.append(line("passive", passive) + "\n");

            out.append("--- an active scan without a login\n");
            List<Alert> anonymous = Zap.scan(target, null, XSS_AND_SQL);
            out.append(line("active, anonymous", anonymous) + "\n");
            List<Flaw> missedAnonymous = Flaw.PLANTED.stream().filter(f -> f.alert().startsWith("Cross Site") && !f.raisedIn(anonymous)).toList();
            out.append("script flaws not found: " + names(missedAnonymous) + "\n");

            out.append("--- an active scan that is logged in\n");
            List<Alert> loggedIn = Zap.scan(target, app.sessionToken(), XSS_AND_SQL);
            out.append(line("active, logged in", loggedIn) + "\n");
            out.append("alerts by risk (high, medium, low, informational): " + byRisk(loggedIn, 3) + ", " + byRisk(loggedIn, 2) + ", " + byRisk(loggedIn, 1) + ", " + byRisk(loggedIn, 0) + "\n");
            out.append("planted flaws found: " + found(loggedIn, true) + "\n");

            out.append("--- the fixed application\n");
            try (BankApp fixed = new BankApp(true)) {
                List<Alert> again = Zap.scan("http://host.docker.internal:" + fixed.port(), fixed.sessionToken(), XSS_AND_SQL);
                out.append(line("active, logged in, after the fixes", again) + "\n");
                out.append("alerts by risk (high, medium, low, informational): " + byRisk(again, 3) + ", " + byRisk(again, 2) + ", " + byRisk(again, 1) + ", " + byRisk(again, 0) + "\n");
                out.append("what is still raised at medium or higher: " + String.join(", ", again.stream().filter(a -> a.risk() >= 2).map(Alert::name).toList()) + "\n");
            }
        }
        out.append("--- the bill\n");
        out.append("a scanner finds what it can reach: without the login the admin area was never tested\n");
        out.append("a clean scan is not a proof: it says these probes found nothing in these pages\n");
        out.append("every alert has a risk and a confidence; a person decides which ones are real\n");
        return out.toString();
    }
}
