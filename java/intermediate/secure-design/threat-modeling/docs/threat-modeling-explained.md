# Threat Modeling with STRIDE, Explained

## The idea in one sentence

A threat model draws the system as elements, flows and trust boundaries, asks the six STRIDE questions of every part, rates and decides each threat, and turns every mitigation into a check, so the model stays true as the design changes.

## The 5 acts

### 1. The system as a data-flow model

The bank is drawn as a data-flow model: 2 external entities, 3 processes, 2 data stores, 8 flows and 3 trust boundaries. The internet boundary is crossed by F2, the gateway boundary by F3 and F8, and the partner boundary by F6 and F7. The model has no problems.
An earlier draft, where the app writes card numbers straight into a card vault, has 2 problems: the flow goes from the device zone to the internal zone with no single boundary between them, and the vault has no owner. A picture that skips a boundary hides the threats that live on it.

### 2. STRIDE per element

STRIDE per element turns the model into questions nobody forgets to ask. The rules find 20 candidate threats: S 4, T 5, R 4, I 2, D 1, E 4. Spoofing for every external entity and every process that receives a flow across a boundary; tampering for every flow that crosses a boundary;
repudiation for every flow that moves money; information disclosure for every store; denial of service for every public-facing process; elevation of privilege for every flow into a more trusted zone. Each category is the opposite of a property: spoofing against authenticity, tampering against integrity, and so on.

### 3. Rating and deciding

Each threat is rated: likelihood follows the least trusted zone it touches, impact follows what is at stake, and risk is their product. The highest risks are at the internet boundary, 5 times 5, 25. Then people decide: mitigate 17, accept 1, transfer 1, avoid 1.
The accepted risk, a card processor denying a charge, is covered by signed receipts and the contract, with an owner and a review on day 90. Denial of service on the gateway is transferred to the hosting provider's protection under contract. Disclosure from the audit log is avoided: card numbers and secrets are never written to it. No threat is left undecided.

### 4. Mitigations as tests

Every mitigation names a check that runs over the model: 8 checks, and every mitigation names one. On the bank as built, none fails. Then a design change adds F9: the payments service sends refunds to the card processor, unencrypted. One check fails now, TLS on every flow that crosses a boundary, naming F9, before the release.
The rules also find 2 new threats with no decision, tampering and repudiation on F9. The earlier draft fails 3 checks, so it would never have shipped either.

### 5. The bill, and the habit

The model is 7 elements, 8 flows and 20 threats: small enough to read in one sitting, and to keep up to date. Today is day 120, and the one accepted risk was due for review on day 90: an accepted risk is a decision with an expiry date.
Review the model when a flow, a store, a boundary or a third party is added, not once a year. Ask the four questions every time: what is being built, what can go wrong, what is being done about it, and was it done well enough. The rules only find candidates; people still judge likelihood, impact and the right control.

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
