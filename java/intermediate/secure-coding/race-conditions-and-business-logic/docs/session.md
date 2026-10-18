# Session Guide — Race Conditions and Business Logic

## Learning Objectives

By the end of the session you can:

- explain check-then-act and why testing one request at a time misses it
- make a check and its action one step with a lock or a compare-and-set
- explain idempotency keys and when a client needs them
- list the business rules a server must enforce whatever the client sends
- explain deadlock and why a lock in one process is not enough with many servers

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Check, then act | 7 min |
| 0:17 | Act 2: Making the check and the write one step | 7 min |
| 0:24 | Act 3: Sending the same request twice | 7 min |
| 0:31 | Act 4: Rules the server must keep | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the two cashiers. Run act one and ask the group to find the step where it went wrong; there is none, only the order. In act two, ask why the compare-and-set needs a retry. In act three, ask who chooses the key,
and what happens if the server chooses it instead. In act four, ask which rules were never written down. In act five, ask how the two servers could share one lock.

## Exercises

1. Change the order string in act one and find every order that overdraws; compare with the count the test computes.
2. Add a daily withdrawal limit, and check that it holds in every order with the lock and with compare-and-set.
3. Give idempotency keys an expiry on a simulated clock, and decide what a retry after expiry should do.
4. Add a three-account transfer and show that one fixed lock order still prevents deadlock.
