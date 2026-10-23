# Session Guide — Static Analysis

## Learning Objectives

By the end of the session you can:

- write a pattern rule and a taint rule
- score a rule on planted flaws and safe lines
- test a rule and see a broken rule fail
- triage a finding with a reason and an owner

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The code under test | 7 min |
| 0:17 | Act 2: Pattern against taint | 7 min |
| 0:24 | Act 3: Tests for the rules | 7 min |
| 0:31 | Act 4: Triage | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and compare the pattern rule with the taint rule in the second act. Open rules/taint.yml and find the sanitizer that the safe parsed number relies on.

## Exercises

1. Add a source for a header and a test for it.
2. Write a rule for a new sink such as a LDAP search and plant a flaw for it.
3. Make the taint rule follow a call into AccountRepository by adding the sink to its method.
4. Add a rule test that fails when the allowlisted column is flagged and decide what to do about it.
