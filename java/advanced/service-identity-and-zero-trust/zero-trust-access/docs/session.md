# Session Guide — Zero Trust Access

## Learning Objectives

By the end of the session you can:

- validate a JWT with a key set, an issuer, an audience and an expiry
- ask a policy engine for an allow or deny with reasons
- write one audit record per request and keep tokens out of it
- explain what a token lifetime and a session check do for revocation

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The perimeter | 7 min |
| 0:17 | Act 2: Who is calling | 7 min |
| 0:24 | Act 3: May they | 7 min |
| 0:31 | Act 4: Logged | 7 min |
| 0:38 | Act 5: A token outlives its session | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and compare the first section with the second: the same 10 requests, 10 successes and then 0. In the audit section find the reason for each denial.

## Exercises

1. Add a rule that lets an auditor read balances only from a compliant device between 08:00 and 20:00.
2. Make the proxy cache the session check for 5 seconds and measure what that does to the revocation window.
3. Add a rate limit per user to the proxy and record it in the audit log.
4. Replace the proxy-to-ledger mark with mutual TLS using the project before this one.
