# Security Requirements with OWASP ASVS, Explained

## The idea in one sentence

A team chooses an ASVS level for its application, writes the requirements that apply down as data in its own words, verifies most of them with checks that run on every build, and reports what is met, failed, not applicable and still manual, so a regression is refused before release.

## The 5 acts

### 1. Choosing a level

A small decision model chooses the level. Every application starts at L1, the floor. A brochure site stays at L1. The online store holds customer accounts and personal data, so it is L2. The online bank also moves money, is regulated and holds highly sensitive data, so it is L3.
The reasons are printed, because a level nobody can explain is a level nobody will defend. Of the 14 requirements, L1 applies 8, L2 applies 12 and L3 applies all 14: each level includes the ones below it.

### 2. Requirements written down as data

The team writes its requirements in a file next to the code, so they are reviewed and versioned like code. security-requirements.tsv holds 14 requirements and reads back exactly the same. For example SR-01, in the Web Frontend Security chapter at L1, verified by an automated test: every page sets a content security policy, nosniff and a framing policy.
Six are verified by an automated test, four by a configuration check, and four by a person. The statements are the team's own words and the ids are the team's own; each one names its chapter of the standard.

### 3. Checks that ask the running store

Each check asks the running store a question and answers pass or fail. Does a page carry a CSP, nosniff and a framing policy? Is the session cookie Secure and HttpOnly? Is a password kept as a slow, salted hash? Does sign-in issue a new session id? Does an error page hide the stack trace? Is the debug endpoint closed, and is the oldest TLS version 1.2 or later?
Are security events logged, and are account pages marked no-store? All of these pass. Do staff sign in with a second factor? That one fails. Nine of ten checks pass on the store as it is configured today. And the same password check on a build that stores passwords as plain SHA-256 fails: a check catches what a review might miss.

### 4. Coverage and gaps

The store at L2: met 8, failed 1, still manual 3, not applicable 2, reported chapter by chapter. The gap is SR-09: staff sign in without a second factor. A release that ships with the debug flag on makes SR-05 and SR-06 newly fail, error pages that show stack traces and an open debug endpoint, and the release gate refuses it.
Once staff sign in with a second factor, nothing fails, and the report says what is left for people to check: 3 requirements.

### 5. The bill

Judged at the bank's L3, the same store has 4 requirements that need a person: SR-08, SR-10, SR-11 and SR-14, and no test replaces that review. A standard says what to verify, not which threats this system faces: the threat model comes first, and its mitigations become extra requirements.
One requirement, SR-14, was mapped against the older edition 4.0.3 of the standard, and is flagged to review the mapping. And a report where every check passes means these checks pass, not that the store is secure.

## The verdict

Choose a level and write down why. Keep the requirements in the repository, in your own words, with the chapter and edition they map to. Automate every requirement that can be checked, run the checks in the build as a release gate, and keep the manual ones visible. Start from the threat model, and treat the standard as the floor, not the ceiling.

## How to recognise this in code you did not write

- A requirements list in a repository with ids such as SR-01 and a chapter for each.
- A build that fails because a security check failed.
- A security report with met, failed and not applicable counts.
- A supplier questionnaire that asks which ASVS level an application meets.

## Where you have already met this

- Security requirements in a tender, a contract or a supplier questionnaire.
- Penetration test reports that cite ASVS requirements.
- Compliance reports that show controls as met, failed or not applicable.
- Release pipelines that fail a build when a security check fails.
