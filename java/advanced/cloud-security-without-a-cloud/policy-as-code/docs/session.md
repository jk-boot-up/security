# Session Guide — Policy as Code

## Learning Objectives

By the end of the session you can:

- write a deny-by-default rule in Rego
- explain why undefined fails open
- test a policy and test its tests
- roll a rule out as a warning

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Deny by default | 7 min |
| 0:17 | Act 2: The holes | 7 min |
| 0:24 | Act 3: Testing the policy and its tests | 7 min |
| 0:31 | Act 4: In the cluster and in the build | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and find the two requests the draft allows. In act three find the four mutants that one test for each rule misses.

## Exercises

1. Add a rule that every Deployment has at least two replicas and its tests.
2. Write the test that kills the cpu limit mutant only.
3. Add an exception list as data with an owner and an end date.
4. Make Conftest fail the build on warnings.
