# Static Analysis, Explained

## The idea in one sentence

A rule is a program, so give it tests and an owner.

## The 5 acts

### 1. The code under test

The bank's web code builds queries from request parameters directly, through a variable and through a helper, runs a converter, reads a file and hashes with MD5. Next to them are safe versions: a prepared statement, a parsed number, a constant, an allowlisted column, a command array, a validated file name and SHA-256.

### 2. Pattern against taint

The pattern rule flags any query that is not a constant: the parsed number, the constant and the allowlisted column too. The taint rule follows the request parameter and knows that parsing is a sanitizer, so it flags only the allowlisted column. It misses the query built in another file, because there the input is a method parameter.

### 3. Tests for the rules

A test file marks the lines a rule must flag and the lines it must leave alone. The four taint checks pass. The same file run against a rule without the parseInt sanitizer fails one check: the test notices the broken rule. The pattern rule has no test, and the run says so.

### 4. Triage

The taint rules report 7 findings, 6 real and 1 false, and exit with code 1. A comment above the false one names the rule, says why and who decides, and the scan reports 6. A second comment with no reason is counted as badly written and the check fails. A file with no flaw gives exit code 0.

### 5. The bill

A rule knows only its sources, sinks and sanitizers, so a check it does not know looks like no check. A rule without a test is a guess. The reports are a list for people to read, not a verdict.

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
