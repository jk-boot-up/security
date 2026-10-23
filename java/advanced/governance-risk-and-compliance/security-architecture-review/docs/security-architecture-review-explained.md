# Security Architecture Review, Explained

## The idea in one sentence

A design review finds what is in the drawing, so draw the trust, not only the boxes.

## The 5 acts

### 1. The design as drawn

The design has a customer, operations staff, a card network, a web application, a payments API, a ledger service, a reporting job and two data stores. Eight of the 9 flows cross a zone. Two of the 4 decision records are accepted. STRIDE asks 2 questions about a person or an external system, 6 about a process, 4 about a data store and 3 about each flow: 65.

### 2. Rules over the design

The rules find the web application reading the ledger database directly, sessions crossing a zone unencrypted, no authentication towards the card network, confidential data unencrypted at rest, a reporting job without an owner, a confidential store without a recorded decision, and two designs resting on decisions that are superseded or only proposed. They miss the ledger calls inside a zone and the hidden form field.

### 3. One more rule

The cross-zone rules cannot see the calls from the API to the ledger, because both are in the application zone. A rule for every flow, inside a zone too, reports them. The tenth weakness, a web application that trusts a hidden form field for the account number, is in no model, so no rule reports it.

### 4. The reviewed design

The web application no longer reads the database: it calls the payments API. Sessions go over TLS with a client certificate, the ledger and card network calls use mutual authentication, the ledger database is encrypted at rest with a recorded decision, the reporting job has an owner, and the superseded and proposed decisions are replaced by accepted ones.

### 5. The bill

A flaw that is not in the picture is not found. The hidden form field needs a person who asks what the web application trusts. A design that the rules agree with is only a design that the rules agree with.

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
