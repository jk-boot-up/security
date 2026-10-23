# Session Guide — Crypto Agility

## Learning Objectives

By the end of the session you can:

- inventory the algorithms used in source code
- design a header that makes stored data self-describing
- migrate data between suites and keys without loss
- explain when a key can be retired and why a backup matters

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: What the code asks for | 7 min |
| 0:17 | Act 2: A value that says how it was protected | 7 min |
| 0:24 | Act 3: Moving the data | 7 min |
| 0:31 | Act 4: Retiring a key | 7 min |
| 0:38 | Act 5: Tampering and downgrade | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and find the 8,000 records left after the 2,000 reads. Then change the write version in the code and run it again to see a rotation to a new suite.

## Exercises

1. Add AES-256-GCM-SIV or another suite as version 4 and rotate to it.
2. Make the batch resumable after a crash.
3. Replace the envelope with a Tink keyset and compare.
4. Write a check for the build that fails on a forbidden algorithm in a new file.
