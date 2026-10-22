# Session Guide — Detecting Attacks with OpenSearch

## Learning Objectives

By the end of the session you can:

- write a detection as a query with a bucket and a threshold
- tune a threshold and read the table
- save a query as a monitor
- say which detections need code

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: One Monday of logs, in OpenSearch | 7 min |
| 0:17 | Act 2: A first version of each rule, as a query | 7 min |
| 0:24 | Act 3: Tuning one threshold | 7 min |
| 0:31 | Act 4: The tuned set as monitors of the alerting plugin | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act three and find the threshold where the false alerts go and the attack stays. In act four find the detection that is not a monitor. In act five find the attack that falls between two buckets.

## Exercises

1. Add a query for logins from a city a user has never used.
2. Change the spraying window to ten minutes and see what happens.
3. Run a monitor without dryrun and read the alert in the plugin's index.
4. Store the queries as JSON files and load them from the files.
