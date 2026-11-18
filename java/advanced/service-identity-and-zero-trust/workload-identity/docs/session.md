# Session Guide — Workload Identity

## Learning Objectives

By the end of the session you can:

- explain why a shared secret makes a leak worth every service
- tell a SPIFFE ID, a registration entry and an SVID apart
- check a peer's SPIFFE ID against a trust bundle in Java
- explain what a stolen SVID is worth and why a short lifetime is the revocation

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: One shared secret in three services | 5 min |
| 0:15 | Act 2: SPIRE decides who a workload is | 5 min |
| 0:20 | Act 3: Two Java services check each other's SPIFFE ID | 5 min |
| 0:25 | Act 4: Rotation: an SVID that lives 20 seconds | 5 min |
| 0:30 | Act 5: A stolen SVID | 5 min |
| 0:35 | Act 6: A workload that matches no entry | 5 min |
| 0:40 | Act 7: Evicting the attacker | 5 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and compare the leaked shared secret with the stolen SVID: what can each do, and for how long? Then change the entry's selector and see who gets the identity.

## Exercises

1. Add a fraud-engine entry that may only GET /statements and call the ledger as it.
2. Change the SVID lifetime to 60 seconds and count the rotations in 50 seconds.
3. Add a second selector (the path of the program) to the payments entry and see what stops matching.
4. Make the ledger refuse an SVID with less than 5 seconds left.
