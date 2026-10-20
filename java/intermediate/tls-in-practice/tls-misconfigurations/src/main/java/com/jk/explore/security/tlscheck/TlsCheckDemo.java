package com.jk.explore.security.tlscheck;

/** Runs the four-server scan in four numbered acts and prints exact, repeatable results. */
public class TlsCheckDemo {

    public static void main(String[] args) {
        System.out.print(run());
    }

    public static String run() {
        StringBuilder out = new StringBuilder();
        Checker checker = new Checker();

        out.append("== 1. A server that follows the guidance ==\n");
        out.append(show(checker.check(Profiles.good(), Profiles.NOW)));

        out.append("\n== 2. A legacy server ==\n");
        Report legacy = checker.check(Profiles.legacy(), Profiles.NOW);
        out.append(show(legacy));

        out.append("\n== 3. A certificate that clients will refuse, and a forgotten HTTP setup ==\n");
        out.append(show(checker.check(Profiles.brokenCertificate(), Profiles.NOW)));
        out.append(show(checker.check(Profiles.forgotHttp(), Profiles.NOW)));

        out.append("\n== 4. Moving the legacy server to a policy ==\n");
        ServerProfile moved = Policy.INTERMEDIATE.applyTo(Profiles.legacy());
        Report after = checker.check(moved, Profiles.NOW);
        out.append("findings before: ").append(legacy.findings().size()).append(", after the intermediate policy: ").append(after.findings().size()).append('\n');
        out.append("left to fix by hand (certificate and HTTP settings):\n");
        after.findings().forEach(f -> out.append("  ").append(f.rule()).append('\n'));
        out.append("the generated nginx fragment:\n");
        Policy.INTERMEDIATE.nginx().lines().forEach(l -> out.append("  ").append(l.length() > 78 ? l.substring(0, 75) + "..." : l).append('\n'));
        return out.toString();
    }

    private static String show(Report r) {
        StringBuilder b = new StringBuilder();
        b.append(String.format("%s: grade %c (score %d), %d finding(s)%n", r.host(), r.grade(), r.score(), r.findings().size()));
        for (Finding f : r.findings()) {
            String line = f.toString();
            b.append("  ").append(line.length() > 100 ? line.substring(0, 97) + "..." : line).append('\n');
        }
        return b.toString();
    }
}
