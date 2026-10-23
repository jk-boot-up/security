package com.jk.explore.security.secretscan;

public class SecretScanningDemo {

    public static void main(String[] args) throws Exception {
        System.out.print(run());
    }

    public static String run() throws Exception {
        StringBuilder out = new StringBuilder();
        Detector own = new Detector();
        Git repo = BankRepo.build();
        out.append("--- the history is the repository\n");
        out.append("commits: " + repo.commits() + ", real secrets planted: " + Planted.realOnes().size() + ", look-alikes: " + (Planted.ALL.size() - Planted.realOnes().size()) + "\n");
        Score ownTree = Score.of(own.scanTree(repo.dir));
        Score ownHistory = Score.of(own.scanHistory(repo));
        Score glTree = Score.of(Gitleaks.files(repo.dir).findings());
        Score glHistory = Score.of(Gitleaks.history(repo.dir).findings());
        out.append("own scanner, files now:     " + ownTree.line() + "\n");
        out.append("own scanner, all history:   " + ownHistory.line() + "\n");
        out.append("gitleaks, files now:        " + glTree.line() + "\n");
        out.append("gitleaks, all history:      " + glHistory.line() + "\n");
        out.append("--- what each one gets wrong\n");
        out.append("own scanner misses: " + Score.names(ownHistory.missed()) + "\n");
        out.append("own scanner flags: " + Score.names(ownHistory.falseAlarms()) + "\n");
        out.append("gitleaks misses: " + Score.names(glHistory.missed()) + "\n");
        out.append("gitleaks flags: " + Score.names(glHistory.falseAlarms()) + "\n");

        out.append("--- an allowlist and a baseline\n");
        java.nio.file.Path config = java.nio.file.Files.createTempFile("gitleaks", ".toml");
        java.nio.file.Files.writeString(config, """
                [extend]
                useDefault = true

                [[allowlists]]
                description = "documentation and test fixtures"
                paths = ['''^docs/''', '''/test/resources/''']
                """);
        Gitleaks.Run allowed = Gitleaks.history(repo.dir, "-c", config.toString());
        out.append("gitleaks with the allowlist: " + Score.of(allowed.findings()).line() + "\n");
        Gitleaks.Run before = Gitleaks.history(repo.dir);
        repo.write("ci.properties", "github.token=ghp_" + "Zk8Yx7Wv6Ut5Sr4Qp3On2Ml1Kj0Ih9Gf8Ed7\n");
        repo.commit("a new token");
        Gitleaks.Run newOnly = Gitleaks.history(repo.dir, "--baseline-path", before.report().toString());
        out.append("findings before the new commit: " + before.findings().size() + ", exit " + before.exit() + "\n");
        out.append("with that report as the baseline, after one more token: " + newOnly.findings().size() + " new, exit " + newOnly.exit() + "\n");

        out.append("--- blocking before the secret leaves the laptop\n");
        Git remote = new Git(java.nio.file.Files.createTempDirectory("remote"));
        remote.must("init", "-q", "--bare", "-b", "main");
        java.nio.file.Path preReceive = remote.dir.resolve("hooks/pre-receive");
        java.nio.file.Files.writeString(preReceive, """
                #!/bin/sh
                while read old new ref; do
                  if [ "$old" = "0000000000000000000000000000000000000000" ]; then range="$new"; else range="$old..$new"; fi
                  gitleaks git -l fatal --no-banner --log-opts="$range" . || exit 1
                done
                """);
        preReceive.toFile().setExecutable(true);
        Git work = BankRepo.clean();
        java.nio.file.Path preCommit = work.dir.resolve(".git/hooks/pre-commit");
        java.nio.file.Files.writeString(preCommit, "#!/bin/sh\nexec gitleaks git --pre-commit --staged -l fatal --no-banner\n");
        preCommit.toFile().setExecutable(true);
        work.must("remote", "add", "origin", remote.dir.toString());
        Sh.Result first = work.run("push", "-q", "origin", "main");
        out.append("the first, clean push: " + (first.ok() ? "accepted" : "rejected") + ", commits there: " + remote.commits() + "\n");
        work.write("app.properties", "payment.key=" + Planted.STRIPE.value() + "\n");
        work.must("add", "-A");
        Sh.Result blocked = work.run("commit", "-q", "-m", "add the key");
        out.append("commit with a secret and the hook: " + (blocked.ok() ? "made" : "refused") + ", commits: " + work.commits() + "\n");
        Sh.Result bypass = work.run("commit", "-q", "--no-verify", "-m", "add the key");
        out.append("the same commit with --no-verify: " + (bypass.ok() ? "made" : "refused") + ", commits: " + work.commits() + "\n");
        Sh.Result pushed = work.run("push", "-q", "origin", "main");
        out.append("push of it to the shared repository: " + (pushed.ok() ? "accepted" : "rejected") + ", commits there: " + remote.commits() + "\n");
        work.must("reset", "-q", "--hard", "HEAD~1");
        work.write("notes.md", "payments notes\n");
        work.commit("a clean change");
        Sh.Result clean = work.run("push", "-q", "origin", "main");
        out.append("a clean change: " + (clean.ok() ? "accepted" : "rejected") + ", commits there: " + remote.commits() + "\n");

        out.append("--- the bill\n");
        Git clone = new Git(java.nio.file.Files.createTempDirectory("clone"));
        Sh.must(java.util.List.of("git", "clone", "-q", repo.dir.toString(), clone.dir.toString()));
        boolean still = !clone.must("log", "--all", "-S" + Planted.STRIPE.value(), "--oneline").isBlank();
        out.append("a fresh clone still holds the payment key that a commit removed: " + (still ? "yes" : "no") + "\n");
        out.append("removing a file does not remove a secret: revoke the key first, then clean up\n");
        out.append("a hook on a laptop can be skipped, a hook on the server cannot\n");
        out.append("every scanner misses something and flags something: measure yours on planted secrets\n");
        return out.toString();
    }
}
