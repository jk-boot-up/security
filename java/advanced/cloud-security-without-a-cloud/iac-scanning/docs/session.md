# Session Guide — IaC Scanning

## Learning Objectives

By the end of the session you can:

- write a rule and test it
- compare scanners by what they miss
- write an exception that ends
- use a baseline to fail on new findings

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Eight rules in Java | 7 min |
| 0:17 | Act 2: Testing the rules | 7 min |
| 0:24 | Act 3: Three scanners on the same files | 7 min |
| 0:31 | Act 4: Exceptions | 7 min |
| 0:38 | Act 5: A baseline, and the bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act three and find the one rule that no scanner has. In act four change the day and watch the finding return.

## Exercises

1. Add a rule for a missing liveness probe and its samples.
2. Add a rule that finds a password written in a ConfigMap.
3. Write the waiver file as YAML and load it.
4. Fail the build when a waiver is stale.
