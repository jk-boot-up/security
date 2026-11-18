package com.jk.explore.security.dast;

import java.util.ArrayList;
import java.util.List;

public class DastDemo {

    static final List<Integer> XSS_AND_SQL = List.of(40012, 40018);

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The kind of a planted flaw, from the alert it should raise. */
    static String kind(Flaw f) {
        return f.alert().startsWith("Cross Site") ? "script" : f.alert().startsWith("SQL") ? "query" : f.alert().startsWith("Cookie") ? "cookie" : "header";
    }

    static long count(List<Alert> alerts, String... kinds) {
        List<String> k = List.of(kinds);
        return Flaw.PLANTED.stream().filter(f -> k.contains(kind(f)) && f.raisedIn(alerts)).count();
    }

    static long planted(String... kinds) {
        List<String> k = List.of(kinds);
        return Flaw.PLANTED.stream().filter(f -> k.contains(kind(f))).count();
    }

    /** What a scan found of the planted flaws, by kind, in two lines. */
    static List<String> found(List<Alert> alerts) {
        return List.of(String.format("script flaws %d of %d, query flaws %d of %d", count(alerts, "script"), planted("script"), count(alerts, "query"), planted("query")),
                String.format("cookie and header flaws %d of %d", count(alerts, "cookie", "header"), planted("cookie", "header")));
    }

    static String names(List<Flaw> flaws) {
        return flaws.isEmpty() ? "none" : String.join(", ", flaws.stream().map(Flaw::id).toList());
    }

    static long byRisk(List<Alert> alerts, int risk) {
        return alerts.stream().filter(a -> a.risk() == risk).count();
    }

    static String risks(List<Alert> alerts) {
        return "high " + byRisk(alerts, 3) + ", medium " + byRisk(alerts, 2) + ", low " + byRisk(alerts, 1) + ", informational " + byRisk(alerts, 0);
    }

    static boolean reachedAdmin(List<Alert> alerts) {
        return alerts.stream().anyMatch(a -> a.paths().stream().anyMatch(p -> p.startsWith("/admin")));
    }

    /** A long piece of a page, cut into lines of at most {@code width} characters, before a tag or an entity where it can. */
    static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        String rest = text;
        while (rest.length() > width) {
            int cut = Math.max(rest.lastIndexOf('&', width), rest.lastIndexOf('<', width));
            if (cut <= 0) {
                cut = width;
            }
            lines.add(rest.substring(0, cut));
            rest = rest.substring(cut);
        }
        lines.add(rest);
        return lines;
    }

    /** A risk or confidence name short enough for a table column. */
    static String shortName(String level) {
        return level.equals("informational") ? "info" : level;
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        List<Alert> passive;
        List<Alert> anonymous;
        List<Alert> loggedIn;
        List<Alert> again;
        Probe.Answer flawedScript;
        Probe.Answer fixedScript;
        List<Probe.Answer> flawedQuery;
        Probe.Answer fixedQuery;
        try (BankApp app = new BankApp(false); BankApp fixed = new BankApp(true)) {
            String target = "http://host.docker.internal:" + app.port();
            passive = Zap.scan(target, null, List.of());
            anonymous = Zap.scan(target, null, XSS_AND_SQL);
            loggedIn = Zap.scan(target, app.sessionToken(), XSS_AND_SQL);
            again = Zap.scan("http://host.docker.internal:" + fixed.port(), fixed.sessionToken(), XSS_AND_SQL);
            flawedScript = Probe.get(app, "/search", "q", Probe.SCRIPT);
            fixedScript = Probe.get(fixed, "/search", "q", Probe.SCRIPT);
            flawedQuery = List.of(Probe.get(app, "/account", "id", "1"), Probe.get(app, "/account", "id", Probe.EXPRESSION), Probe.get(app, "/account", "id", "2"));
            fixedQuery = Probe.get(fixed, "/account", "id", Probe.EXPRESSION);
        }

        out.append("ONE. One attack, worked out by hand.\n");
        out.append("  THE REQUEST (the attack string of ZAP's rule 40012):\n");
        out.append("    GET /search?q=" + Probe.SCRIPT + "\n");
        out.append("  THE RESPONSE OF THE FLAWED APPLICATION:\n");
        out.append("    status " + flawedScript.status() + ", the page holds:\n");
        wrap(flawedScript.content(), 58).forEach(l -> out.append("    " + l + "\n"));
        out.append("    the attack string came back unchanged: " + (Probe.reflected(flawedScript) ? "yes" : "no") + "\n");
        out.append("  THE RESPONSE OF THE FIXED APPLICATION:\n");
        out.append("    status " + fixedScript.status() + ", the page holds:\n");
        wrap(fixedScript.content(), 58).forEach(l -> out.append("    " + l + "\n"));
        out.append("    the attack string came back unchanged: " + (Probe.reflected(fixedScript) ? "yes" : "no") + "\n");
        Alert xss = loggedIn.stream().filter(a -> a.name().startsWith("Cross Site")).findFirst().orElseThrow();
        Alert.Hit onSearch = xss.hits().stream().filter(h -> h.path().equals("/search")).findFirst().orElseThrow();
        out.append("  WHAT ZAP REPORTS:\n");
        out.append("    " + xss.name() + "\n");
        out.append("    on " + onSearch.path() + ", parameter " + onSearch.param() + ", the same attack: " + (onSearch.attack().equals(Probe.SCRIPT) ? "yes" : "no") + "\n");
        out.append("    risk " + xss.riskName() + ", confidence " + xss.confidenceName() + "\n");

        out.append("TWO. The application, and a passive scan.\n");
        out.append("  THE APPLICATION UNDER TEST:\n");
        out.append("    planted flaws " + Flaw.PLANTED.size() + ", behind the login " + Flaw.PLANTED.stream().filter(Flaw::needsLogin).count() + "\n");
        for (Flaw f : Flaw.PLANTED) {
            out.append(String.format("    %-7s %-33s %s%n", kind(f), f.id(), f.path() == null ? "every page" : f.path()));
        }
        out.append("  THE PASSIVE SCAN (crawl, then only read the responses):\n");
        out.append("    attacks sent: none\n");
        out.append("  WHAT IT FOUND:\n");
        found(passive).forEach(l -> out.append("    " + l + "\n"));

        out.append("THREE. An active scan, without a login.\n");
        out.append("  THE PLAN ADDS (two active rules):\n");
        out.append("    40012 reflected script, 40018 SQL injection\n");
        out.append("  HOW THE QUERY CHECK WORKS (worked out by hand):\n");
        String[] ids = {"1", Probe.EXPRESSION, "2"};
        for (int i = 0; i < ids.length; i++) {
            out.append(String.format("    GET /account?id=%-4s %s%n", ids[i], flawedQuery.get(i).content()));
        }
        out.append("    3-2 gives the page of 1: the database worked it out: " + (Probe.computed(flawedQuery.get(0), flawedQuery.get(1), flawedQuery.get(2)) ? "yes" : "no") + "\n");
        out.append("    the fixed application answers id=3-2 with status " + fixedQuery.status() + "\n");
        out.append("  WHAT IT FOUND:\n");
        found(anonymous).forEach(l -> out.append("    " + l + "\n"));
        out.append("  WHAT IT MISSED:\n");
        out.append("    " + names(Flaw.PLANTED.stream().filter(f -> !f.raisedIn(anonymous)).toList()) + "\n");
        out.append("    the admin area reached: " + (reachedAdmin(anonymous) ? "yes" : "no") + "\n");

        out.append("FOUR. An active scan that is logged in.\n");
        out.append("  THE PLAN ADDS (a replacer rule):\n");
        out.append("    every request sends Cookie: session=<the login's value>\n");
        out.append("  WHAT IT FOUND:\n");
        found(loggedIn).forEach(l -> out.append("    " + l + "\n"));
        out.append("    the admin area reached: " + (reachedAdmin(loggedIn) ? "yes" : "no") + "\n");
        out.append("  HOW THE TOTAL IS MADE:\n");
        out.append("    " + count(loggedIn, "script") + " + " + count(loggedIn, "query") + " + " + count(loggedIn, "cookie", "header") + " = "
                + Flaw.PLANTED.stream().filter(f -> f.raisedIn(loggedIn)).count() + " of " + Flaw.PLANTED.size() + " planted flaws\n");
        out.append("  THE REPORT (" + loggedIn.size() + " alerts):\n");
        out.append(String.format("    %-7s%-45s%s%n", "risk", "alert", "confidence"));
        for (Alert a : loggedIn.stream().sorted(java.util.Comparator.comparingInt(Alert::risk).reversed().thenComparing(Alert::name)).toList()) {
            out.append(String.format("    %-7s%-45s%s%n", shortName(a.riskName()), a.name(), a.confidenceName()));
        }

        out.append("FIVE. The fixed application.\n");
        out.append("  THE FIXES:\n");
        out.append("    output escaped, the query bound to a number,\n");
        out.append("    cookie HttpOnly and SameSite, security headers sent\n");
        out.append("  BEFORE THE FIXES (the logged-in scan):\n");
        out.append("    planted flaws raised " + Flaw.PLANTED.stream().filter(f -> f.raisedIn(loggedIn)).count() + " of " + Flaw.PLANTED.size() + "\n");
        out.append("    " + risks(loggedIn) + "\n");
        out.append("  AFTER THE FIXES (the same scan):\n");
        out.append("    planted flaws raised " + Flaw.PLANTED.stream().filter(f -> f.raisedIn(again)).count() + " of " + Flaw.PLANTED.size() + "\n");
        out.append("    " + risks(again) + "\n");
        out.append("  LEFT FOR A PERSON TO JUDGE (medium or higher):\n");
        for (Alert a : again.stream().filter(a -> a.risk() >= 2).toList()) {
            out.append("    " + a.name() + " on " + String.join(", ", a.paths()) + "\n");
            out.append("    risk " + a.riskName() + ", confidence " + a.confidenceName() + "\n");
        }
        return out.toString();
    }
}
