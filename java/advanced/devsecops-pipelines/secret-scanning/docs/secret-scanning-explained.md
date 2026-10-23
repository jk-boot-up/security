# Secret Scanning, Explained

## The idea in one sentence

A secret in the history is a secret in every clone, so scan the history and revoke what you find.

## The 5 acts

### 1. The history is the repository

The bank's repository has 5 commits. Two files with keys were deleted by a later commit. A scan of the files at HEAD finds 2 of the 5 real secrets, and a scan of every line ever added finds them all with the small scanner and 4 with gitleaks.

### 2. What each one gets wrong

Four look-alikes are planted: a key in the documentation, a checksum, a test key in a fixture and an identifier. The small scanner flags the documentation key and the test key. gitleaks flags the test key and misses the database password, which has too little entropy.

### 3. An allowlist and a baseline

An allowlist for the documentation folder and test resources leaves 4 of 5 real secrets found and no look-alike flagged. A report of the 7 findings saved as a baseline lets the next run report only the 1 new token, with exit code 1 so the build fails on it.

### 4. Blocking the next one

A hook that runs gitleaks on the staged changes refuses a commit with a payment key. The same commit made with --no-verify succeeds. A pre-receive hook in the shared repository scans the pushed commits and rejects the push; a clean change is accepted.

### 5. The bill

The payment key was removed from the files by a later commit and a fresh clone still holds it. Removing a file does not remove a secret: revoke the key first, then clean up. Every scanner misses and flags, and a hook on a laptop can be skipped.

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
