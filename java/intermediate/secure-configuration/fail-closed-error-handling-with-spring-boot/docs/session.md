# Session Guide — Failing Closed with Spring Boot

## Learning Objectives

By the end of the session you can:

- explain why a decision must start as no
- give every call to another service a budget, a limited retry and a breaker
- name the two rules that make @Transactional leave a transfer half done
- give the customer a reference and keep the reason in the log

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Failing open | 7 min |
| 0:17 | Act 2: Failing closed | 7 min |
| 0:24 | Act 3: Failing halfway | 7 min |
| 0:31 | Act 4: What the customer is told | 7 min |
| 0:38 | Act 5: The bill, and how to pay it | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act one and ask where in your own code a catch-all sits. In act three ask which of the four transfers would pass a code review. In act five ask who would hear the alert at night.

## Exercises

1. Add a daily-limit check that fails closed when its value cannot be read, and test it.
2. Replace the hand-written breaker with a library breaker and compare the counts.
3. Make the transfer idempotent with a request id, and retry it safely.
4. Add a health indicator that goes DOWN when the breaker is open.
