# Session Guide — Backups and Recovery with an S3 Object Store

## Learning Objectives

By the end of the session you can:

- explain what versioning protects against, and what it does not
- explain the difference between COMPLIANCE and GOVERNANCE locks
- give a backup job the least privilege it needs
- list the costs of immutable backups

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Versioning: an overwrite does not destroy the old data | 7 min |
| 0:17 | Act 2: Object Lock: nobody can delete a locked version | 7 min |
| 0:24 | Act 3: The backup job gets an identity that can only add objects | 7 min |
| 0:31 | Act 4: A checksum catches a damaged backup | 7 min |
| 0:38 | Act 5: Keeping history without keeping it for ever, and the cost | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the safe deposit box. Run the demo one act at a time. After act one, ask what an attacker with full rights could still do. After act two, ask who should be allowed to bypass a GOVERNANCE lock. In act three, ask what the
thief who steals the backup key can still do, and what would reveal it.

## Exercises

1. Change the retention to ten seconds on a GOVERNANCE lock and watch it expire, then try the same with COMPLIANCE.
2. Give the backup identity read access to one prefix and test what it can then reach.
3. Add a legal hold to an object and see what it refuses.
4. Set a bucket default retention and check that new objects get it without being asked.
