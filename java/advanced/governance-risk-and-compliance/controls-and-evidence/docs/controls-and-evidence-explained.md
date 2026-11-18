# Controls and Evidence, Explained

## The idea in one sentence

A control you cannot show is a control you cannot claim.

## The 5 acts

### 1. One control, mapped to two frameworks

The input is one line of catalogue/controls.csv: id C03, name "TLS on every internal call", owner platform-team, csf PR.DS-02, iso A.8.24;A.8.20, evidence tls-everywhere.xml, kind auto (a test report).
A CSF 2.0 id reads function, category, subcategory: PR is Protect, DS is Data Security, 02 is the second outcome of that category. An Annex A id reads theme and control: A.8.24 is theme 8 (technological), control 24, use of cryptography; A.8.20 is networks security.
The result: one promise answers both frameworks, and one test report is the evidence for all three references.

### 2. The maps: every control, both frameworks

The input is the catalogue of 12 controls with 7 owners, and the statement of applicability: the 14 Annex A controls the bank chose.
The CSF side: Govern C11 C12, Identify C05 C06, Protect C01 C02 C03 C04 C10, Detect C07, Respond C09, Recover C08. Every function has at least one control.
The ISO side: 13 of the 14 are served; A.8.28, secure coding, is not served by any control. C03 serves A.8.24 and A.8.20, and C08 serves A.8.13 and A.5.30.
The result: 6 of 6 functions, 13 of 14 Annex A controls, one gap.

### 3. Evidence from a test report

The input is the JUnit XML test report of the build: a testsuite element with the name TlsEverywhereTest, tests="10", failures="1", skipped="0" and the timestamp 2026-10-21T09:00:00; one testcase, rejectsTlsOneOne, holds a failure element.
The reader takes the suite, the date of the timestamp, the counts and the names of the failed tests.
The status is decided by asking the rules in order: any evidence? yes. Fresh? 2026-10-23 - 2026-10-21 = 2 days, within the limit of 90. Any test failed? 1 of 10, rejectsTlsOneOne, so the status is FAILING, and the failing test is named.

### 4. Every control today

The input is the whole catalogue on one day, 2026-10-23: seven controls with a test report, four signed by a person, one with nothing.
The rules, in order: no evidence is NONE; evidence older than 90 days (365 for a signature) is STALE; a signature is ATTESTED; a failed test is FAILING; a skipped test is PARTIAL; anything else is PASSING.
The result for each control: C01, C02, C05 and C08 are passing; C03 failing; C07 partial, with 2 of 8 tests skipped; C04 stale even though its 15 tests passed, because they ran 144 days ago; C10 stale, signed 448 days ago; C09, C11 and C12 attested; C06 has no evidence. Fresh passing test evidence: 4 of 12.

### 5. When a test starts to fail

Before: C01, least privilege roles, has LeastPrivilegeTest with 24 tests and none failed, so it is PASSING; the Protect function has 2 of 5 controls with passing evidence (C01 and C02).
The change: a role change lets a clerk act as an admin, and the next build runs the same 24 tests, one of which fails.
After: any test failed? 1 of 24, deniesAnAdminRoleToAClerk, so C01 is FAILING, and Protect has 1 of 5 (C02). The status changed with the build.
What evidence cannot show: a passing test proves the test passed, not that the control works, and a test can be weak; an attestation is a promise that ages slowly and proves less; a control with no owner and no date is a wish.

## The verdict

Give every control an owner and a mapping, derive its status from evidence with a date, set freshness limits, let the build change the status, and read a green result as a claim about the test as well.

## How to recognise this in code you did not write

- A control matrix in a spreadsheet with last year's date.
- A screenshot as the only evidence.
- A framework mapping nobody has reviewed.
- A statement of applicability with a control that no one serves.

## Where you have already met this

- A spreadsheet of controls updated before the audit.
- A screenshot as evidence.
- A control that serves three standards and has three owners.
- A statement of applicability nobody has read since it was signed.
