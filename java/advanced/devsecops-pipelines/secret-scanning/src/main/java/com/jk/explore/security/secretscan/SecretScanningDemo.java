package com.jk.explore.security.secretscan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

public class SecretScanningDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    /** The commit a finding came from, by number ("commit 3"), or nothing for a scan of files. */
    static int commitNumber(List<String> ids, Finding f) {
        return ids.indexOf(f.commit()) + 1;
    }

    /** The commit that added a planted string, and the one that removed it (0 when it is still there). */
    static int[] addedRemoved(List<Git.Change> changes, Planted p) {
        int added = changes.stream().filter(c -> c.added() && p.in(c.line())).mapToInt(Git.Change::commit).min().orElse(0);
        int removed = changes.stream().filter(c -> !c.added() && p.in(c.line())).mapToInt(Git.Change::commit).min().orElse(0);
        return new int[] {added, removed};
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Detector own = new Detector();
        Git repo = BankRepo.build();
        List<String> ids = repo.ids();
        List<Git.Change> changes = repo.changes();

        out.append("ONE. One string, measured by hand.\n");
        String encoded = Base64.getEncoder().encodeToString(Planted.BASE64.value().getBytes());
        int[] k8s = addedRemoved(changes, Planted.BASE64);
        out.append("  THE INPUT (k8s/secret.yaml, commit " + k8s[0] + "):\n");
        out.append("    key: " + encoded.substring(0, 16) + "... (" + encoded.length() + " characters, base64)\n");
        out.append("    pattern rules that match it: " + Detector.RULES.stream().filter(r -> r.pattern().matcher(encoded).find()).count() + " (base64 hides the prefix)\n");
        out.append("  HOW THE ENTROPY IS MADE (bits per character):\n");
        out.append("    H = the sum of p x log2(1/p), p = count / " + encoded.length() + "\n");
        for (EntropySteps.Group g : EntropySteps.groups(encoded)) {
            out.append("    " + g.line() + "\n");
        }
        out.append(String.format("    H = %.3f, the threshold %.1f: a secret%n", EntropySteps.total(encoded), Detector.THRESHOLD));
        out.append("  A LOOK-ALIKE (checksums.txt, a SHA-256 in hex):\n");
        out.append("    " + Planted.HASH.value().substring(0, 16) + "... (" + Planted.HASH.value().length() + " characters, " + EntropySteps.distinct(Planted.HASH.value()) + " different)\n");
        out.append(String.format("    at most log2(16) = 4.000, H = %.3f: below %.1f%n", EntropySteps.total(Planted.HASH.value()), Detector.THRESHOLD));
        out.append("  WHAT THE DETECTOR REPORTS:\n");
        for (String text : List.of("key: " + encoded, "payments.jar " + Planted.HASH.value())) {
            List<Finding> f = own.scanText("x", text);
            out.append("    " + (text.startsWith("key") ? "the base64 key  " : "the checksum    ") + (f.isEmpty() ? "nothing" : f.get(0).rule()) + "\n");
        }
        out.append("    decoded, the key is " + Planted.BASE64.shown() + " (" + Planted.BASE64.id() + ")\n");

        out.append("TWO. The files now, and the whole history.\n");
        out.append("  THE REPOSITORY (" + repo.commits() + " commits):\n");
        String[] subjects = repo.must("log", "--reverse", "--format=%s").strip().split("\n");
        for (int i = 0; i < subjects.length; i++) {
            out.append("    commit " + (i + 1) + "  " + subjects[i] + "\n");
        }
        out.append("  THE PLANTED STRINGS (" + Planted.realOnes().size() + " real, " + (Planted.ALL.size() - Planted.realOnes().size()) + " look-alikes):\n");
        for (Planted p : Planted.ALL) {
            int[] ar = addedRemoved(changes, p);
            out.append(String.format("    %-4s  %-28s added %d, %s%n", p.real() ? "real" : "fake", p.id(), ar[0], ar[1] == 0 ? "still there" : "removed " + ar[1]));
        }
        Score ownTree = Score.of(own.scanTree(repo.dir));
        Score ownHistory = Score.of(own.scanHistory(repo));
        Score glTree = Score.of(Gitleaks.files(repo.dir).findings());
        Gitleaks.Run glRun = Gitleaks.history(repo.dir);
        Score glHistory = Score.of(glRun.findings());
        out.append("  A SCAN OF THE FILES NOW (real secrets found):\n");
        out.append("    our scanner " + ownTree.found().size() + " of 5, gitleaks " + glTree.found().size() + " of 5\n");
        out.append("  A SCAN OF THE WHOLE HISTORY (real secrets found):\n");
        out.append("    our scanner " + ownHistory.found().size() + " of 5, gitleaks " + glHistory.found().size() + " of 5\n");
        Git clone = new Git(Files.createTempDirectory("clone"));
        Sh.must(List.of("git", "clone", "-q", repo.dir.toString(), clone.dir.toString()));
        boolean still = !clone.must("log", "--all", "-S" + Planted.STRIPE.value(), "--oneline").isBlank();
        out.append("  A FRESH CLONE:\n");
        out.append("    holds the payment key that commit " + addedRemoved(changes, Planted.STRIPE)[1] + " removed: " + (still ? "yes" : "no") + "\n");

        out.append("THREE. What each scanner gets wrong.\n");
        out.append("  THE 9 STRINGS, AND WHAT EACH SCANNER SAYS:\n");
        out.append(String.format("    %-33s%-9s%s%n", "the truth", "ours", "gitleaks"));
        for (Planted p : Planted.ALL) {
            out.append(String.format("    %-33s%-9s%s%n", (p.real() ? "real " : "fake ") + p.id(), ownHistory.verdict(p), glHistory.verdict(p)));
        }
        out.append("  HOW THE SCORE IS MADE:\n");
        out.append(String.format("    %-28s%-12s%s%n", "", "ours", "gitleaks"));
        out.append(String.format("    %-28s%-12s%s%n", "secrets found", ownHistory.found().size() + " of 5", glHistory.found().size() + " of 5"));
        out.append(String.format("    %-28s%-12s%s%n", "look-alikes flagged", ownHistory.falseAlarms().size() + " of 4", glHistory.falseAlarms().size() + " of 4"));
        out.append(String.format("    %-28s%-12s%s%n", "recall = found / real", ownHistory.recall(), glHistory.recall()));
        out.append(String.format("    %-28s%-12s%s%n", "precision = real / flagged", ownHistory.precision(), glHistory.precision()));
        out.append("  WHY THE PASSWORD IS MISSED BY GITLEAKS:\n");
        out.append(String.format("    db.password=%s: %d characters, H = %.3f%n", Planted.PASSWORD.shown(), Planted.PASSWORD.value().length(), Detector.entropy(Planted.PASSWORD.value())));
        out.append("    no known prefix and little entropy; our scanner has a\n");
        out.append("    rule for the word password, so it finds it\n");

        out.append("FOUR. An allowlist and a baseline.\n");
        Path config = Files.createTempFile("gitleaks", ".toml");
        Files.writeString(config, """
                [extend]
                useDefault = true

                [[allowlists]]
                description = "documentation and test fixtures"
                paths = ['''^docs/''', '''/test/resources/''']
                """);
        out.append("  THE ALLOWLIST (gitleaks.toml):\n");
        out.append("    paths ^docs/ and /test/resources/ are not reported\n");
        out.append("  BEFORE THE ALLOWLIST:\n");
        out.append("    " + glHistory.line() + "\n");
        Score allowed = Score.of(Gitleaks.history(repo.dir, "-c", config.toString()).findings());
        out.append("  AFTER THE ALLOWLIST:\n");
        out.append("    " + allowed.line() + "\n");
        out.append("  THE BASELINE (today's report, " + glRun.findings().size() + " findings, exit " + glRun.exit() + "):\n");
        for (Finding f : glRun.findings().stream().sorted(java.util.Comparator.comparingInt((Finding f) -> commitNumber(ids, f)).thenComparing(Finding::file).thenComparing(Finding::rule)).toList()) {
            out.append(String.format("    commit %d  %-20s%s%n", commitNumber(ids, f), Path.of(f.file()).getFileName(), f.rule()));
        }
        repo.write("ci.properties", "github.token=ghp_" + "Zk8Yx7Wv6Ut5Sr4Qp3On2Ml1Kj0Ih9Gf8Ed7\n");
        repo.commit("a new token");
        List<String> after = repo.ids();
        Gitleaks.Run newOnly = Gitleaks.history(repo.dir, "--baseline-path", glRun.report().toString());
        out.append("  AFTER ONE MORE COMMIT, WITH THAT BASELINE:\n");
        out.append("    findings " + newOnly.findings().size() + " new, exit " + newOnly.exit() + "\n");
        for (Finding f : newOnly.findings()) {
            out.append(String.format("    commit %d  %-20s%s%n", commitNumber(after, f), Path.of(f.file()).getFileName(), f.rule()));
        }

        out.append("FIVE. Blocking a secret before it is shared.\n");
        String gitleaks = Gitleaks.binary().toString();
        Git remote = new Git(Files.createTempDirectory("remote"));
        remote.must("init", "-q", "--bare", "-b", "main");
        Path preReceive = remote.dir.resolve("hooks/pre-receive");
        Files.writeString(preReceive, """
                #!/bin/sh
                while read old new ref; do
                  if [ "$old" = "0000000000000000000000000000000000000000" ]; then range="$new"; else range="$old..$new"; fi
                  GITLEAKS git -l fatal --no-banner --log-opts="$range" . || exit 1
                done
                """.replace("GITLEAKS", gitleaks));
        preReceive.toFile().setExecutable(true);
        Git work = BankRepo.clean();
        Path preCommit = work.dir.resolve(".git/hooks/pre-commit");
        Files.writeString(preCommit, "#!/bin/sh\nexec " + gitleaks + " git --pre-commit --staged -l fatal --no-banner\n");
        preCommit.toFile().setExecutable(true);
        work.must("remote", "add", "origin", remote.dir.toString());
        out.append("  THE HOOKS (both run gitleaks):\n");
        out.append("    pre-commit   on the laptop, scans what is staged\n");
        out.append("    pre-receive  on the shared repository, scans each push\n");
        Sh.Result first = work.run("push", "-q", "origin", "main");
        out.append("  A CLEAN FIRST PUSH:\n");
        out.append("    " + (first.ok() ? "accepted" : "rejected") + ", commits on the server " + remote.commits() + "\n");
        work.write("app.properties", "payment.key=" + Planted.STRIPE.value() + "\n");
        work.must("add", "-A");
        out.append("  THE INPUT: A COMMIT WITH THE PAYMENT KEY (app.properties)\n");
        Sh.Result blocked = work.run("commit", "-q", "-m", "add the key");
        out.append("    with the pre-commit hook: " + (blocked.ok() ? "made" : "refused") + ", commits on the laptop " + work.commits() + "\n");
        Sh.Result bypass = work.run("commit", "-q", "--no-verify", "-m", "add the key");
        out.append("    with --no-verify: " + (bypass.ok() ? "made" : "refused") + ", commits on the laptop " + work.commits() + "\n");
        Sh.Result pushed = work.run("push", "-q", "origin", "main");
        out.append("  THE RESULT: THE PUSH OF THAT COMMIT\n");
        out.append("    pre-receive: " + (pushed.ok() ? "accepted" : "rejected") + ", commits on the server " + remote.commits() + "\n");
        work.must("reset", "-q", "--hard", "HEAD~1");
        work.write("notes.md", "payments notes\n");
        work.commit("a clean change");
        Sh.Result clean = work.run("push", "-q", "origin", "main");
        out.append("  AFTER THE KEY IS TAKEN OUT, A CLEAN CHANGE:\n");
        out.append("    " + (clean.ok() ? "accepted" : "rejected") + ", commits on the server " + remote.commits() + "\n");
        return out.toString();
    }
}
