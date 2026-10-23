# Controls and Evidence, Explained

## The idea in one sentence

A control you cannot show is a control you cannot claim.

## The 5 acts

### 1. The catalogue and its maps

Each control has an owner, one CSF 2.0 subcategory and one or more Annex A controls. Governance has 2, Identify 2, Protect 5, Detect 1, Respond 1 and Recover 1. The statement of applicability chose 14 Annex A controls; A.8.28, secure coding, is not served by any control. Two controls serve two Annex A controls each.

### 2. What the evidence says today

Seven controls have test results, four are attested by a person and one has nothing. Today there are 4 passing, 1 partial (two skipped tests), 1 failing, 2 stale, 3 attested and 1 with no evidence. Automated evidence is stale after 90 days, an attestation after 365.

### 3. The trail for one control

C03, TLS on every internal call, belongs to the platform team, serves CSF PR.DS-02 and Annex A A.8.24 and A.8.20, and its last run was 2 days ago: 10 tests, 1 failed. The oldest automated evidence is the configuration baseline, 144 days old against a limit of 90.

### 4. When a test starts to fail

C01, least privilege, is passing. With one failing test it is failing. The Protect function had 2 of 5 controls with passing evidence; it now has 1 of 5. A status that follows the tests needs no one to remember to update a spreadsheet.

### 5. The bill

A test can be too weak, so a green result is a claim about the test as much as the control. A person's attestation is evidence of a promise. A control with no owner and no date is a wish, and every line of the catalogue has both.

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
