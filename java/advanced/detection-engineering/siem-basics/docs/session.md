# Session Guide — SIEM Basics

## Learning Objectives

By the end of the session you can:

- parse three log formats into one event shape
- explain why lines that do not parse must be counted
- show how a time zone mistake breaks a correlation
- write a rule that needs two sources

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Collecting three sources | 7 min |
| 0:17 | Act 2: Normalizing into one shape | 7 min |
| 0:24 | Act 3: The clock | 7 min |
| 0:31 | Act 4: Correlating across sources | 7 min |
| 0:38 | Act 5: From alerts to cases | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and compare the web-only query with the common query. Then set the offset to be ignored and see the second rule go quiet.

## Exercises

1. Add a fourth source, a proxy log, and its parser.
2. Add enrichment: the department of each user and the criticality of each host.
3. Make the rule window per user depend on the user's usual behaviour.
4. Alert when the number of dead letters rises.
