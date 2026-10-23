# Session Guide — Privacy Engineering

## Learning Objectives

By the end of the session you can:

- compare the personal data collected with the purposes that need it
- measure how easily people are picked out of data without names
- run a retention job that respects a legal hold
- erase a customer and search for the data that remains

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: What is collected, and why | 7 min |
| 0:17 | Act 2: Who can be picked out | 7 min |
| 0:24 | Act 3: Pseudonyms for analytics | 7 min |
| 0:31 | Act 4: Retention | 7 min |
| 0:38 | Act 5: A customer's request | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and compare the two lines about being alone in the group. In the retention act find the copy that the job does not reach.

## Exercises

1. Add a rule that fails the build when a column is collected for no purpose.
2. Make the analytics extract k-anonymous with k of 5 and count the rows that must be dropped.
3. Add the analytics table to the retention job and the data map.
4. Add a field for the lawful basis of each purpose and a check for it.
