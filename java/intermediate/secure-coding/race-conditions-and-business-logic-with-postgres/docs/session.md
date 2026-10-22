# Session Guide — Race Conditions with PostgreSQL and Hibernate

## Learning Objectives

By the end of the session you can:

- explain the gap between a check and a write
- close it with a constraint, a lock, an isolation level or a version
- say what a caller must do when the database refuses with 40001
- name two ways a lock can hurt

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Check, then act: two requests in the gap | 7 min |
| 0:17 | Act 2: Row locks: making the check and the write one step | 7 min |
| 0:24 | Act 3: Isolation levels | 7 min |
| 0:31 | Act 4: Optimistic locking with Hibernate, and one-time keys | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act one and find the line where both checks pass. In act three find the level at which write skew passes. In act five find the lock that protects nothing.

## Exercises

1. Add a unique constraint and use it to make a coupon single-use.
2. Write a retry loop with a limit and a pause for 40001 and 40P01.
3. Add FOR NO KEY UPDATE to a query and see which other statements it blocks.
4. Find the long transaction in pg_stat_activity and give it a timeout.
