# Session Guide — Secrets in Errors and Logs

## Learning Objectives

By the end of the session you can:

- list what an error page must not contain and what a reference gives support
- explain log injection and the one-line-per-event rule
- describe what redaction can and cannot find, with an example of each
- write a canary test for a flow

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: What the customer sees when the database is down | 8 min |
| 0:18 | Act 2: What the log keeps | 8 min |
| 0:26 | Act 3: A generic answer, and the details where only the team looks | 8 min |
| 0:34 | Act 4: Redaction, wrappers and forged lines | 8 min |
| 0:42 | Exercises | 18 min |

## Walkthrough

Run act one and ask what a visitor learns. In act two, ask who can read the log, for how long. In act four, find a secret the redactor cannot recognise, and say what would catch it.

## Exercises

1. Add a field for a customer's e-mail address to the log and mask all but the first letter and the domain.
2. Replace the free-text lines with structured key-value logging and redact by key.
3. Write a canary test that puts a fake password through the whole checkout and scans the sink.
4. Add a rule that rejects `Secret.reveal()` outside one package, using a test that scans the source.
