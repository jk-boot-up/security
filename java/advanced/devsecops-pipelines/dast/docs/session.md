# Session Guide — Dynamic Analysis

## Learning Objectives

By the end of the session you can:

- tell a passive scan from an active scan
- give a scanner a session so it reaches the pages behind a login
- count a scan against planted flaws
- explain why a clean scan is not a proof

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The application under test | 7 min |
| 0:17 | Act 2: Passive and anonymous scans | 7 min |
| 0:24 | Act 3: A logged-in scan | 7 min |
| 0:31 | Act 4: The fixed application | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and compare the anonymous scan with the logged-in scan. Find the one flaw the anonymous scan could not reach.

## Exercises

1. Add a flaw that needs a header, plant it and see which scan finds it.
2. Add the rule for path traversal to the active scan.
3. Make the scan fail the build on any high alert.
4. Add a second user and scan as both.
