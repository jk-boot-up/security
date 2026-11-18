# Security Chaos Engineering, Explained

## The idea in one sentence

Break the control on purpose, count what the service does, and make sure someone is told.

## The 7 acts

### 1. The service, the faults and the steady state

The input: a bank payments service with five controls, each a call to a dependency: the rate limit (ratelimit), the certificate revocation check (revocation), authorization, the fraud score (fraud) and the audit trail (audit). The first version, when a call fails, reads it as no limit, not revoked, allow, a clean score, and drops the audit record.
Toxiproxy 2.13.0 sits between the service and each dependency and can inject six faults: down (connections refused), slow (answers held back 5000 ms), hang (accepted, nothing sent), reset, trickle (1 KB per second) and slow close (3000 ms after the answer). Every call has a deadline of 1500 ms, body included.
The steady state, checked before each experiment in both versions: s1 and s2 are approved, s3 (9000) is denied by the fraud score, s4 (certificate rev-7) is denied by the revocation check, and 4 of 4 audit records arrive. It held before 20 of 20 runs, so every difference later is caused by the fault.

### 2. Authorization: down, slow, reset

The hypotheses: no payment is approved while authorization is down (E1), answers too late (E2) or has its connection reset (E3). Six payments are made in each experiment.
The first version reads an unanswered call as allow: approved 6 of 6 in all three, and the control is BROKEN. The hardened version reads it as a deny: approved 0 of 6 in all three, and the control holds.

### 3. The fraud score: a hang and a trickle

The hypotheses: no payment is approved without a fraud score when the fraud service hangs (E4) or trickles (E5).
The first version reads no score as a clean score: unscreened 6 of 6, review 0, BROKEN. The hardened version holds the payment for a person: unscreened 0 of 6, review 6, held.
How the trickle misses the deadline: an answer is 4009 bytes, the score line and a padding; at 1024 bytes a second that is 4009 / 1024 = 3.9 s, more than the 1.5 s deadline. A deadline on the connection alone would wait for every byte.

### 4. The audit trail: the sink down, then a slow close

The hypotheses: every decision reaches the audit sink once it is back (E6), and a slow close loses no record and raises no false alarm (E7).
The first version keeps a buffer of 5 records in memory and drops what cannot be sent: in E6 the 20 payments are approved and 20 of 20 records are lost; dropped 20, and nobody is told. The hardened version writes each record to a journal on disk first, then sends it: 20 wait in the journal while the sink is down, and 20 are sent when it is back, so 0 of 20 are lost.
The slow close (E7) costs nothing: 4 approved, 0 of 4 lost, in both versions.

### 5. The rate limit and the revocation check

The hypotheses: one account never gets more than 5 approved in a burst (E8), and a revoked certificate is never approved when the responder is down (E9) or late (E10).
The first version has no limit without its limiter and reads no revocation answer as good: approved 40 of 40, and revoked approved 6 of 6 twice; all BROKEN. The hardened version counts locally and refuses without an answer: approved 5 of 40, and revoked approved 0 of 6.
How the local count holds the limit: payments 1 to 5 bring the count to 1 to 5, which is at most 5, so they are approved; payments 6 to 40 bring it to 6 to 40, more than 5, so they are refused.

### 6. Does anybody hear it?

The monitor probes the health endpoint of each of the 5 dependencies once a round; 3 failed probes in a row raise one alert, and an experiment runs 4 rounds. The delay is counted in probes, not seconds, so it is the same on every machine.
In E1, authorization down, the probes fail 1, 2, 3 in a row and the third raises the alert; the fourth adds none. In E7, the slow close, all 4 rounds are answered and nothing is raised.
9 outages need an alert and E7 needs none. The first version has no monitor and raises 0 of 9; the hardened version raises 9 of 9, each after 3 failed probes, with 0 false alarms.

### 7. The scoreboard, and what is left

The scoreboard lists the 10 experiments with two verdicts each, the control and the alert. 10 experiments x 2 hypotheses = 20. The first version holds 2 of 20 (both in E7) and breaks 18; the hardened version holds 20 of 20.
The gap the hardened version still has: a certificate revoked while the responder is down, whose last cached answer was good, is approved 1 of 1.
The bill: no answer is still a decision, and the first version said yes; a dropping audit buffer fails in silence and only a count finds it; without probes, 9 of the 9 outages were never reported; and failing closed has a price, because payments wait or are refused while a dependency is down.

## The verdict

List the controls that call a dependency, write a hypothesis for each, inject the failures with a proxy, count approvals, losses and unscreened payments, add a probe-based alert for every dependency, and keep the scoreboard in the build.

## How to recognise this in code you did not write

- A catch block that returns the permissive answer.
- A bounded in-memory queue in front of an audit sink.
- A revocation check described as best effort.
- A dashboard nobody was paged from.

## Where you have already met this

- A try and catch that returns true.
- A timeout that falls back to allow.
- A log buffer that drops when it is full.
- A revocation check that soft-fails.
