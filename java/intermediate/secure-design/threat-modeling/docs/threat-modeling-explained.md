# Threat Modeling with STRIDE, Explained

## The idea in one sentence

A threat model draws the system as elements, flows and trust boundaries, asks the six STRIDE questions of every part, rates and decides each threat, and turns every mitigation into a check, so the model stays true as the design changes.

## The 5 acts

### 1. The system as a data-flow model

The input is the bank as a data-flow model: the customer E1 and the mobile app P1 in the device zone (trust 0), the API gateway P2 in the dmz (trust 1, public), the card processor E2 in the partner zone (trust 1), and the payments service P3, the accounts database S1 and the audit log S2 in the internal zone (trust 2). Three trust boundaries separate device | dmz, dmz | internal and internal | partner. Eight flows connect them; F2 crosses the internet boundary, F3 and F8 the gateway boundary, F6 and F7 the partner boundary.
The result of the check of the picture: the bank as built has 0 problems. An earlier draft adds S3 card vault (a data store in internal, owner: none) and D1, the card number from P1 to S3: D1 goes from device to internal, skipping a boundary, and S3 has no owner, so 2 problems.

### 2. STRIDE per element

The input is one flow, F2, the payment request with token from the mobile app to the API gateway. The result of the six questions: spoofing, information disclosure and denial of service are asked of entities, processes and stores, not of flows; tampering applies because F2 crosses the internet boundary; repudiation because it moves money; elevation of privilege because it goes from device (trust 0) to dmz (trust 1).
The same rules on the whole model find 20 candidate threats: S 4 (E1 E2 P2 P3), T 5 (F2 F3 F6 F7 F8), R 4 (F2 F3 F4 F6), I 2 (S1 S2), D 1 (P2) and E 4 (F2 F3 F7 F8). Six examples are printed, one per letter.

### 3. Rating and deciding

How one threat is rated, worked out for T:F2: the likelihood follows the less trusted end of the flow, and the weaker end of F2 is the device zone, so 5 (device 5, dmz 4, partner 3, internal 2); the impact is 5 because F2 moves money (other flows 3); the risk is 5 x 5 = 25.
The input of the decision is the 20 threats on the risk matrix: likelihood 5 has 1 threat at impact 4 and 3 at impact 5, and so on. The top three, ties in key order, are E:F2, R:F2 and T:F2, all 5 x 5 = 25.
The result is one decision per threat: mitigate 17 (each names a check), accept 1 (R:F6, signed receipts and the contract cover it, owner payments lead, review on day 90), transfer 1 (D:P2 to the hosting provider, under contract), avoid 1 (I:S2, no card numbers in the audit log), undecided 0.

### 4. Mitigations as tests

Before the change, the bank as built: 0 of 8 checks fail and 0 threats are undecided. The change adds F9: the payments service P3 sends refunds to the card processor E2, across the partner boundary, unencrypted.
After the change: 1 of 8 checks fails, TLS on every boundary-crossing flow, because F9 refund is not encrypted; and the rules find 2 new threats with no decision, T:F9 and R:F9. The test fails before the release.
All 8 checks on the three versions of the model: as built every one passes; with F9 only TLS fails; the early draft fails three (both ends authenticated, stores encrypted and reached only from inside, every store has an owner), broken by D1 card number not authenticated, S3 not encrypted at rest, D1 reaching the vault from the device zone, and S3 having no owner.

### 5. The bill, and the habit

The size of the model: 7 elements, 8 flows, 3 boundaries, 20 threats and 8 checks, small enough to read in one sitting.
The accepted risk R:F6 was accepted on day 0 with a review on day 90; today is day 120, so the review is 120 - 90 = 30 days overdue: an accepted risk is a decision with an expiry date.
Open the model again for a new flow, store, boundary or third party, not once a year, and ask the four questions: what is being built (the model), what can go wrong (STRIDE), what is being done about it (the decisions), and was it done well enough (the checks).

## The verdict

Model early, keep it small, and keep it in the repository next to the code. Decide every threat explicitly, give accepted risks an owner and a date, and make mitigations testable. Revisit the model with every design change, using the four questions.

## How to recognise this in code you did not write

- A data-flow diagram with dashed trust boundaries in a design document.
- A risk register with owners, decisions and review dates.
- A pull request template that asks whether the threat model changed.
- OWASP Threat Dragon, pytm or the Microsoft Threat Modeling Tool.

## Where you have already met this

- Design reviews and architecture decision records that ask "what could go wrong?"
- Data-flow diagrams in security reviews, with dashed trust boundaries.
- Tools such as OWASP Threat Dragon, pytm and the Microsoft Threat Modeling Tool.
- Risk registers with owners and review dates.
