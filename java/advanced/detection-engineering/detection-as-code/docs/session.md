# Session Guide — Detection as Code

## Learning Objectives

By the end of the session you can:

- write a Sigma rule with a condition, a filter and ATT&CK tags
- score a rule against labelled logs
- set and check a false-alarm budget
- test a rule with cases and mutants
- read an ATT&CK coverage matrix

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The recorded logs | 5 min |
| 0:15 | Act 2: Rules scored | 5 min |
| 0:20 | Act 3: False-positive budgets | 5 min |
| 0:25 | Act 4: Tests and mutants | 5 min |
| 0:30 | Act 5: ATT&CK coverage | 5 min |
| 0:35 | Act 6: pySigma and OpenSearch | 5 min |
| 0:40 | Exercises | 20 min |

## Walkthrough

Run the demo. Find the rule that is over budget, remove its filter in a copy, and watch the tests and the false alarms change.

## Exercises

1. Write a ninth rule for T1021.004 and add its attacks and tests.
2. Add a filter to log-clearing to bring it within budget and a test that says why.
3. Catch the evasions of download-pipe-shell and see the false alarms.
4. Add the Elasticsearch backend and compare queries.
