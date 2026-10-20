# Session Guide — API Security: Mass Assignment, Data Exposure and Resource Limits

## Learning Objectives

By the end of the session you can:

- explain mass assignment and how an explicit request model stops it
- explain excessive data exposure and why a response model is a security control
- choose a page cap and say why a list costs more than one row
- describe a token bucket, 429 and Retry-After

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A profile call that works | 7 min |
| 0:17 | Act 2: Whatever the caller sends becomes a field | 7 min |
| 0:24 | Act 3: The response is the whole record, and a list has no end | 7 min |
| 0:31 | Act 4: Explicit models, a page cap and a budget | 7 min |
| 0:38 | Act 5: The budget runs out, and refills | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and ask who decided that "role" was a field the caller could set. In act three, count the fields in the response and ask which the app really uses. In act five, ask what a caller who
sends one request every 0.6 seconds would see.

## Exercises

1. Add a `phone` field to the request model with a pattern, and refuse an unknown country code.
2. Charge a transfer more tokens than a read, and show the effect on a loop of transfers.
3. Return a JSON error with the name of the refused field, and decide whether that is safe to show.
4. Add a maximum request body size and refuse larger bodies with 413.
