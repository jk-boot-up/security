# Session Guide — Threat Hunting

## Learning Objectives

By the end of the session you can:

- turn a hypothesis about an intruder into a query
- judge a lead against what really happened
- explain why benign periodic traffic looks like a beacon
- turn a hunt into a rule with an allowlist

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The week | 7 min |
| 0:17 | Act 2: Hunt 1: a timer in the lookups | 7 min |
| 0:24 | Act 3: Hunts 2 and 3: pairs and accounts | 7 min |
| 0:31 | Act 4: Hunt 4 and the scoreboard | 7 min |
| 0:38 | Act 5: From a hunt to a rule | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and compare the first two lines of hunt 1. Then change the limit on regularity and see when the beacon appears and disappears.

## Exercises

1. Add a hunt for logins from two places at once.
2. Add jitter detection that does not depend on a fixed limit.
3. Write the rare pair hunt as a rule and backtest it on the clean week.
4. Add the scheduled task to the logs and write the hunt that finds it.
