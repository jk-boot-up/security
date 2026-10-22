# Session Guide — Secrets in Logs with Logback

## Learning Objectives

By the end of the session you can:

- show what a careless message leaves in a log
- add a conversion word to a Logback layout
- write a type that cannot print its value
- say what a mask cannot cover

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: What the log keeps | 7 min |
| 0:17 | Act 2: Covering the shapes of secrets in the layout | 7 min |
| 0:24 | Act 3: Make the secret unprintable | 7 min |
| 0:31 | Act 4: Structured logs | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act one and list every secret in the file. In act three find the line that needed no mask. In act five find the secret that no mask knew.

## Exercises

1. Add a conversion word that hashes the user id instead of printing it.
2. Add a path mask for an address field.
3. Write a test that fails when a Secret reaches a log line through string concatenation.
4. Put the JSON events in a file and search them for a card number.
