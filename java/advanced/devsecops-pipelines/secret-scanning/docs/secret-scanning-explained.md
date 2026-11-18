# Secret Scanning, Explained

## The idea in one sentence

A secret in the history is a secret in every clone, so scan the history and revoke what you find.

## The 5 acts

### 1. One string, measured by hand

The input is the key line of k8s/secret.yaml, added in commit 5: 44 characters of base64, which hides the sk_live_ prefix, so none of the pattern rules matches it.
How the entropy is made: H is the sum of p times log2 of 1/p over the characters, with p the count divided by 44. Characters that appear the same number of times are added as one group: 21 once give 2.606, 6 twice give 1.216, 2 three times give 0.528 and 1 five times gives 0.357. H = 4.707, above the threshold of 4.2.
A look-alike: the checksum is a SHA-256 in hex, 64 characters of only 16 different symbols, so at most log2(16) = 4.000; it measures 3.786.
The result: the detector reports high-entropy-string for the key and nothing for the checksum. Decoded, the key is the payment key in a Kubernetes Secret.

### 2. The files now, and the whole history

The input: five commits, from the first version of the payments service to a key stored in a Kubernetes Secret, with commit 4 removing the secrets from the files. Five real secrets and four look-alikes were planted; the demo works out, from git log, the commit that added each one and the one that removed it.
The result: a scan of the files now finds 2 of 5 with our scanner and with gitleaks, because three secrets were removed. A scan of every line ever added finds 5 of 5 with our scanner and 4 of 5 with gitleaks.
A fresh git clone still holds the payment key that commit 4 removed: yes.

### 3. What each scanner gets wrong

The input: the 9 planted strings, with the truth (real or fake) and what each scanner said about each one over the whole history.
How the score is made: recall divides the secrets found by the 5 real ones (ours 5/5 = 100%, gitleaks 4/5 = 80%); precision divides them by every planted string flagged (ours 5/7 = 71%, gitleaks 4/5 = 80%).
Why gitleaks misses the database password: db.password=Summer2024! is 11 characters with an entropy of 3.096, no known prefix and little randomness; our scanner has a rule for an assignment to a word like password.

### 4. An allowlist and a baseline

The input: an allowlist that skips the documentation and the test resources, and today's report as a baseline.
Before the allowlist, gitleaks finds 4 of 5 and flags 1 of 4 look-alikes; after it, 4 of 5 and 0 of 4.
The baseline holds 7 findings, listed by commit and file: the payment key in app.properties and the test key in fixture.properties (commit 1), the cloud key (commit 2), the source token (commit 3) and the key in secret.yaml, reported by three rules (commit 5).
After one more commit with a new token, gitleaks reports only that one: 1 new finding in commit 6, exit code 1.

### 5. Blocking a secret before it is shared

The hooks: pre-commit on the laptop scans what is staged; pre-receive on the shared repository scans each push. Both run gitleaks.
The input: a commit that adds the payment key to app.properties. With the pre-commit hook it is refused, and the laptop has 1 commit; with --no-verify it is made, and the laptop has 2.
The result: the push of that commit is rejected by the pre-receive hook, and the server keeps 1 commit. After the key is taken out, a clean change is accepted and the server has 2 commits.

## The verdict

Scan the whole history once and every change after that, measure the scanner on planted secrets, keep the allowlist small, block at the client and at the server, and revoke before you clean up.

## How to recognise this in code you did not write

- A secrets file deleted in a later commit.
- A hook skipped with --no-verify.
- A scan that only reads the files at HEAD.
- An allowlist that grows every sprint.

## Where you have already met this

- A password in application.properties "for now".
- A key removed by a commit that is still in the log.
- A hook that everyone skips with --no-verify.
- A scanner that nobody has measured.
