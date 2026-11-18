# Session Guide — Security Chaos Engineering

## Learning Objectives

By the end of the session you can:

- write a chaos experiment as a steady state, an injected failure, an observation and a verdict
- inject down, slow, hung, reset, trickle and slow-close failures with Toxiproxy from Java
- tell fail open from fail closed and count what each approves, loses or leaves unscreened
- measure alert latency in failed probes and keep a scoreboard of hypotheses

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The service, the faults and the steady state | 5 min |
| 0:15 | Act 2: Authorization: down, slow, reset | 5 min |
| 0:20 | Act 3: The fraud score: a hang and a trickle | 5 min |
| 0:25 | Act 4: The audit trail: the sink down, then a slow close | 5 min |
| 0:30 | Act 5: The rate limit and the revocation check | 5 min |
| 0:35 | Act 6: Does anybody hear it? | 5 min |
| 0:40 | Act 7: The scoreboard, and what is left | 5 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and read E1 for both versions. Then find the experiment where both versions hold, and say why a slow close is not a failure the probes should report.

## Exercises

1. Add a circuit breaker to authorization and rerun E1 to E3.
2. Add an experiment that makes the audit sink slow instead of down.
3. Make the cache of revocation answers expire after a number of payments and measure the gap again.
4. Write the runbook entry for payments held for review.
