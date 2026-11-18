# Security Requirements with OWASP ASVS, Explained

## The idea in one sentence

A team chooses an ASVS level for its application, writes the requirements that apply down as data in its own words, verifies most of them with checks that run on every build, and reports what is met, failed, not applicable and still manual, so a regression is refused before release.

## The 5 acts

### 1. Choosing a level

The input is three applications, each described by what it holds and does: a brochure site with LOW data, no accounts, no money and no regulation; the online store with MEDIUM data and customer accounts; the online bank with HIGH data, accounts, money and regulation.
The result: every application starts at L1, the floor; holding customer accounts or personal data raises it to L2; moving money under regulation with HIGH data raises it to L3. So the brochure site is L1, the store L2 and the bank L3, and each level comes with the reasons that gave it. Of the 14 requirements, L1 asks for 8 (SR-01 to SR-08), L2 for 12 (L1 + SR-09, SR-10, SR-12, SR-14) and L3 for all 14 (L2 + SR-11, SR-13).

### 2. Requirements written down as data

The input is the team's 14 requirements, SR-01 to SR-14, each with the lowest level it applies to, how it is verified (automated, configuration or manual) and its chapter of the standard, such as SR-01 L1 automated Web Frontend Security.
The result is security-requirements.tsv: 15 lines, a header and 14 rows, read back as the same 14 requirements: true. One row, field by field: id SR-01, chapter Web Frontend Security, level L1, verification AUTOMATED, check headers, edition 5.0, statement "Every page sets CSP, nosniff and a framing policy". Automated 6 (SR-01 to SR-05, SR-13), configuration 4 (SR-06, SR-07, SR-09, SR-12), manual 4 (SR-08, SR-10, SR-11, SR-14).

### 3. Checks that ask the running store

The input is ten questions, one per check, such as password-storage: is a password a slow, salted hash? The store as configured today answers: headers, cookies, password-storage, session-rotation, error-leak, debug-off, tls, audit-log and no-store pass; staff-mfa fails. 9 of 10 pass.
How the password check decides: the stored value has the shape scheme$rounds$salt$hash, and it needs pbkdf2-sha256 with rounds >= 600,000. Before, the store today stores pbkdf2-sha256$600000$ followed by a 16-byte salt and a 32-byte hash: pbkdf2-sha256, 600,000 >= 600,000 rounds, pass. After, a build that uses plain SHA-256 stores sha256$c4bbcb1fbec99d65bf59d85c...: no rounds and no salt, FAIL; the same password gives the same 64 hex digits on every store.

### 4. Coverage and gaps

The input is the 14 requirements, the store's level L2, and the answers of the checks. The result, requirement by requirement: met 8 (SR-01 to SR-07, SR-12), failed 1 (SR-09), not applicable 2 (SR-11, SR-13, which are L3), manual 3 (SR-08, SR-10, SR-14). By chapter, Authentication has 1 met and 1 failed; Authorization, Validation and Business Logic, and Secure Coding and Architecture each have 1 manual.
The gap is SR-09: staff accounts sign in with a second factor. Before the release, failing SR-09. After a release with the debug flag on, failing SR-05 SR-06 SR-09, so newly failing SR-05 SR-06, and the release gate refuses it. After staff sign in with a second factor, failing 0, and SR-08 SR-10 SR-14 are left to people.

### 5. The bill

The same store, with staff signing in with a second factor, judged at two levels: at L2, 12 apply, 9 met, 0 failed, 3 manual; at L3, 14 apply, 10 met, 0 failed, 4 manual. At L3 four requirements need a person, and no test replaces it: SR-08 every order and account access is checked on the server; SR-10 business limits, like refunds per day, hold on the server; SR-11 signing keys live in a hardware module or a key service; SR-14 third-party components are listed and kept up to date.
SR-14 is mapped to edition 4.0.3, older than 5.0, so its mapping must be reviewed. And a passing report does not say which threats this system faces (the threat model does), nor that the store is secure: only that these checks pass.

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
