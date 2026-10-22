# Session Guide — API Security with Spring Boot and Redis

## Learning Objectives

By the end of the session you can:

- explain mass assignment and fix it with a request record that fails on unknown fields
- return only what a caller may see and give every list a ceiling
- write a rate limiter that every server shares
- decide what the API does when the limiter cannot answer

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The first version | 7 min |
| 0:17 | Act 2: A request model | 7 min |
| 0:24 | Act 3: A response model and a page ceiling | 7 min |
| 0:31 | Act 4: The budget runs out, and refills | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act one and find the field you would least like a customer to set. In act four ask why the script takes the time as an argument. In act five ask what a refused request costs the customer, and what an unbounded one costs the bank.

## Exercises

1. Add a fourth field, phone, to the request record with a pattern, and test good and bad numbers.
2. Give the PATCH route a smaller budget than the GET routes.
3. Make the limiter fail open for reads and closed for writes, and test both.
4. Add a per-IP bucket for callers who are not signed in.
