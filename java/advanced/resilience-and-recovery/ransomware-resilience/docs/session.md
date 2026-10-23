# Session Guide — Ransomware Resilience

## Learning Objectives

By the end of the session you can:

- show what an attacker with the backup credentials can destroy in a plain, a versioned and a locked bucket
- give the backup job and the recovery account separate, minimal policies
- keep a copy in a second store with its own credentials
- stop poisoned backups with a canary file and entropy, and restore the last good run
- compute RTO and RPO for a restore exercise

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A plain bucket | 5 min |
| 0:15 | Act 2: Versioning only | 5 min |
| 0:20 | Act 3: Object lock in COMPLIANCE mode | 5 min |
| 0:25 | Act 4: Isolation and an air-gapped copy | 5 min |
| 0:30 | Act 5: Ransomware and the poisoned backup | 5 min |
| 0:35 | Act 6: The restore exercise | 5 min |
| 0:40 | Exercises | 20 min |

## Walkthrough

Run the demo and follow the count of restorable backups through the first three acts. In the fifth act find the line where the poisoned backup matches its manifest and say why that does not help. In the last act change the throughput and see which restore misses the target.

## Exercises

1. Change the retention from 1 day to 30 and decide what a poisoned run costs to keep.
2. Make the ransomware encrypt 10 percent of each file and find the block size at which the test stops seeing it.
3. Add a second recovery step that replays the ledger and compares the total with the manifest.
4. Write the same policies as OpenTofu files and apply them to the store.
