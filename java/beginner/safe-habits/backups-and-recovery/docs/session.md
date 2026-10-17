# Session Guide — Backups and Recovery

## Learning Objectives

By the end of the session you can:

- explain what a manifest proves and what it does not
- say why a restore test is the only proof
- apply the 3-2-1 rule to a setup
- explain why ransomware reaches a mounted backup and what does not
- calculate a recovery point and a recovery time

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Full and incremental backups, with a manifest | 7 min |
| 0:17 | Act 2: Can the backup be trusted? Only a restore test says | 7 min |
| 0:24 | Act 3: The 3-2-1 rule, and how long to keep backups | 7 min |
| 0:31 | Act 4: Ransomware, and the copy it cannot change | 7 min |
| 0:38 | Act 5: How much can be lost, how long it takes, and backups as secrets | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the fire drill. Run the demo one act at a time. After act two, ask the group what else a restore test should check. In act four, ask who can write to each copy and what a stolen shop account could do.
In act five, ask what the shop can afford to lose and what that does to the schedule.

## Exercises

1. Add a restore test for the stock file that checks every quantity is a number.
2. Change the retention policy to 14 daily, 8 weekly and 24 monthly and see the number kept.
3. Add a fourth copy to the careful setup and see which rule it improves.
4. Make the nightly backup hourly in the timeline and find the cost in storage of the extra snapshots.
