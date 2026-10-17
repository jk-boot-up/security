# Session Guide — Failing Closed: Secure Error Handling

## Learning Objectives

By the end of the session you can:

- explain failing open and failing closed, with an example of each
- describe why a decision should start as no
- make a two-step change all or nothing, and release resources on every path
- write an error response that helps support and not an attacker
- explain what a circuit breaker, a time budget and a retry each add

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Failing open | 7 min |
| 0:17 | Act 2: Failing closed | 7 min |
| 0:24 | Act 3: Failing halfway | 7 min |
| 0:31 | Act 4: What the customer is told, and what the log keeps | 7 min |
| 0:38 | Act 5: The bill, and how to pay it | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the vault in a power cut. Run the demo one act at a time. After act one, ask the group to find a catch-all in their own code. After act two, ask what the customer at the till
sees and how support helps them. In act three, ask what would happen if the undo itself failed. In act five, ask who should be woken up when refusals rise.

## Exercises

1. Add a balance service that can also fail, and make the payment check refuse when it cannot be reached.
2. Make the transfer idempotent with a request id, so a retried transfer is not applied twice.
3. Add exponential back-off to the retry, on the simulated clock, and cap the total time.
4. Write a test that fails if any class in the project has an empty catch block.
