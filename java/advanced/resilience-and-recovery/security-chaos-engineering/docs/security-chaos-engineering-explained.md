# Security Chaos Engineering, Explained

## The idea in one sentence

Break the control on purpose, count what the service does, and make sure someone is told.

## The 5 acts

### 1. Steady state

A chaos experiment starts from a steady state that is checked, not assumed. Two good payments are approved, a risky one is refused by the fraud score and a payment with a revoked certificate is refused, and 4 of 4 audit records reach the sink. Both versions pass this in all 10 experiments, so any later difference is caused by the failure.

### 2. Authorization and fraud

Toxiproxy refuses the connection, holds every answer back 5 seconds, resets the connection, accepts and sends nothing, or trickles the answer at 1 KB per second, while the service has a deadline of under a second. The first version reads an unreachable authorization service as yes and an unreachable fraud service as a clean score. The hardened version denies, or holds the payment for review.

### 3. Audit, limit and revocation

With the audit sink down for a burst of 20 decisions the first version drops them from a small buffer and nobody knows; the hardened version writes a journal first and sends it when the sink is back. When the rate-limit store resets connections the first version has no limit; the hardened one counts in its own process. When the revocation responder is down or slow the first version reads silence as not revoked; the hardened one refuses.

### 4. Does anybody hear it?

A monitor probes the health endpoint of every dependency through its proxy. Three failed probes in a row raise one alert, so the delay is counted in probes and does not depend on the clock. The first version has no monitor, so 9 of the 9 outages that needed an alert were never reported. The slow close of the audit sink does not fail a probe, and the hardened version raises no false alarm for it.

### 5. The scoreboard

The scoreboard lists the 10 experiments with two verdicts each. The first version holds 2 of 20 hypotheses and the hardened version 20 of 20. The hardened version still has a gap that the experiments measure: a certificate whose last cached answer was good and which is revoked while the responder is down is approved, 1 of 1. Fail closed also has a price: the payments are refused or wait in review while a dependency is down.

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
