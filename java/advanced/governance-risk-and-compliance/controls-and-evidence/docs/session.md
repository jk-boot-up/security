# Session Guide — Controls and Evidence

## Learning Objectives

By the end of the session you can:

- map a control to NIST CSF 2.0 and ISO 27001
- read evidence from JUnit test results
- decide a control's status from the evidence and its age
- follow the trail from a control to a failing test

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The catalogue and its maps | 7 min |
| 0:17 | Act 2: What the evidence says today | 7 min |
| 0:24 | Act 3: The trail for one control | 7 min |
| 0:31 | Act 4: When a test starts to fail | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and find the Annex A control that no control serves. Then change a date in the evidence folder and see a control go stale.

## Exercises

1. Add a control for secure coding and serve A.8.28 with the sast project's tests.
2. Add a rule that a control with a failing test for more than 7 days is escalated.
3. Map the catalogue to SOC 2 criteria as a third framework.
4. Read the evidence from the real test results of the earlier projects.
