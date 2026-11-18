# Security Architecture Review, Explained

## The idea in one sentence

A design review finds what is in the drawing, so draw the trust, not only the boxes.

## The 5 acts

### 1. The design as drawn

The input is the file design/v1/bank.dsl. Each part has a kind, a zone and an owner, and a data store has a classification: the ledger database holds confidential data. Each flow says whether it is encrypted and how it checks its caller.
The result: 9 parts in 6 zones, 9 flows, 8 of them crossing a zone and 1 inside one. Four decision records: 0001 and 0002 accepted, 0003 superseded by 5, 0004 only proposed.
How the 65 STRIDE questions add up: 3 people or external systems times 2 letters (S,R) is 6; 4 processes times all 6 letters is 24; 2 data stores times 4 letters (T,R,I,D) is 8; 9 flows times 3 letters (T,I,D) is 27. 6 + 24 + 8 + 27 = 65.

### 2. Rules over the design

The input: the ten planted weaknesses, the truth the rules are scored on, and the first eight rules, each a few lines of Java over the model.
How a flow is checked: web -> ledgerdb goes from dmz to data, is encrypted and checks its caller with a database user, but it reaches confidential data from an outer zone, so no-direct-path-to-data fires. web -> sessions crosses the same zones but is not encrypted, so cross-zone-encrypted fires. How a decision is checked: sessions rests on 0003, which is Superseded by 5, and cards rests on 0004, which is Proposed: neither is Accepted.
The result: 9 findings. 8 match a planted weakness by rule and subject; wide-access-to-data on ledgerdb (3 containers reach it) matches none. Missed: the ledger calls inside the app zone, and the hidden account-number field.

### 3. One more rule

The input: one more rule, every-flow-encrypted-and-authenticated. The cross-zone rules skip api -> ledger, because both ends are in the app zone; it is not encrypted and checks no caller.
The result: 3 new findings. api -> ledger is planted weakness 4; api -> cards and web -> sessions were already found by the cross-zone rules. With 9 rules: 12 findings, 9 of 10 planted weaknesses found, 3 findings that match no planted one, 1 missed.
The tenth weakness, a web application that trusts a hidden form field for the account number, is in no model: the model has no property for what a page trusts, so no rule can read it.

### 4. The reviewed design

Before the review (design/v1): 9 parts, 9 flows of which 8 cross a zone, 12 findings from all 9 rules, 1 flow from the dmz to confidential data, 2 of 4 decision records accepted.
The changes, worked out by comparing the two design files: the ledger database is encrypted at rest and cites decision 0001; sessions now rest on 0005; the reporting job is owned by the payments team; the flow web -> ledgerdb is removed; the sessions, ledger and card network flows are encrypted and check a client certificate; decision 0003 is removed, 0004 goes from Proposed to Accepted, and 0005 is added as Accepted.
After the review (design/v2): 9 parts, 8 flows of which 7 cross a zone, 0 findings, 0 flows from the dmz to confidential data, 4 of 4 decision records accepted.

### 5. The bill

What the rules gave: 9 rules, 9 of 10 planted weaknesses found, 0 findings in the reviewed design.
What is still left to people: the reviewed design still raises 62 STRIDE questions (3 x 2 + 4 x 6 + 2 x 4 + 8 x 3), one planted weakness no rule can find, whether the built system matches the model, and whether the accepted decisions are right.
Rules read the model, and the model is what the team drew. No findings means the rules agree, not that the system is safe.

## The verdict

Write the design as text with zones, owners and flow properties, check it with rules on every change, record decisions with a status, score the rules on planted weaknesses, and keep a person in the review to ask what is missing.

## How to recognise this in code you did not write

- A diagram with boxes and no trust boundaries.
- A decision made once and never revisited.
- A review that only checks the diagram against itself.
- A data store reached from the web tier.

## Where you have already met this

- A diagram on a whiteboard photographed once.
- A design document nobody has opened since approval.
- A decision made in chat and never written down.
- A review that checks the diagram but not the code.
