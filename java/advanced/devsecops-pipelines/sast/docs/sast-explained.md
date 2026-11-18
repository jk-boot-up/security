# Static Analysis, Explained

## The idea in one sentence

A rule is a program, so give it tests and an owner.

## The 5 acts

### 1. One flaw, as a taint rule sees it

The input is three lines of the bank's AccountController, lines 22 to 24, and the rule sql-taint from rules/taint.yml: source $REQ.getParameter(...), sink $S.executeQuery(...), sanitizers Integer.parseInt(...) and Validate.fileName(...).
How the taint travels, worked out by hand: line 22, name is tainted because it comes from the source; line 23, sql is tainted because it is built from name; line 24, executeQuery(sql) gets a tainted value, which is a finding.
The result: Semgrep reports sql-taint at AccountController:24, the same line.

### 2. Pattern against taint

The input: six files with 7 planted flaws and 7 safe look-alikes, each marked // VULN or // SAFE, and two rules: the pattern rule sql-concat matches executeQuery($Q + ...) and executeQuery($Q); the taint rule sql-taint follows getParameter to executeQuery.
The result, line by line on the 8 SQL lines: the pattern rule finds every flaw but also flags the parsed number, the constant and the allowlisted column. The taint rule knows parsing is a sanitizer and that a constant carries no input; it flags only the allowlisted column, and misses sql-other-file, whose query is built in AccountRepository, another file.
How the score is made: recall = found / flaws (4/4 = 100% and 3/4 = 75%); precision = real / flagged (4/7 = 57% and 3/4 = 75%).

### 3. Every flaw class with the taint rules

The input: rules/taint.yml with three taint rules (sql-taint, command-taint, path-taint) and one pattern rule for MD5 and SHA-1.
The result: 7 findings, each matched with the marker on its line: sql-direct, sql-variable, sql-helper, weak-hash, command and path are real; sql-allowlisted is a false alarm.
By class: sql 3 of 4 flaws found and 1 of 4 safe lines flagged; command 1 of 1 and 0 of 1; path 1 of 1 and 0 of 1; weak-hash 1 of 1 and 0 of 0; all together 6 of 7 and 1 of 7.

### 4. Tests for the rules

The input: a test file next to the rules, rules/taint.java, where // ruleid: <rule> marks a line the rule must flag (6 of them) and // ok: <rule> a line it must not flag (5).
Before the change, semgrep --test compares the expected lines with the reported ones: command-taint 32 and 32, path-taint 42 and 42, sql-taint 10 16 and 10 16, weak-hash 52 54 and 52 54: 4 of 4 pass. pattern.yml has no test file.
The change: the sanitizer Integer.parseInt(...) is removed from sql-taint. After it, sql-taint reports 10 16 22 where 10 16 were expected, so it fails and 3 of 4 pass. Line 22 runs a query with a number parsed from the request, marked // ok: the broken rule now flags safe code, and the test says so.

### 5. Triage

The input: the taint rules run with --error, which makes the exit code 1 when there is a finding, so the build fails: 7 findings, 6 real and 1 false.
The suppression: a comment above AccountController line 56 with three parts: the rule (nosemgrep: rules.sql-taint), the reason (the column is checked against an allowlist above) and the owner (payments-team).
The result: 6 findings and still exit code 1, because the real flaws remain; suppressions 1, well written 1. A second suppression with no reason gives suppressions 2, well written 1, and the build check fails. A file with no flaw, Validate.java, gives 0 findings and exit code 0.

## The verdict

Start with taint rules for the flaws that matter, score every rule on planted flaws and safe look-alikes, test the rules in the build, suppress only with a reason and an owner, and keep a list of what the tool cannot see.

## How to recognise this in code you did not write

- A scan that reports a thousand findings and nobody reads.
- A suppression comment with no reason.
- A custom rule without a test file.
- A report that is green because the rule saw nothing.

## Where you have already met this

- A scanner report with a thousand findings and no owner.
- A grep for "executeQuery" in a build step.
- A suppression comment with no reason.
- A rule pack that nobody has tested.
