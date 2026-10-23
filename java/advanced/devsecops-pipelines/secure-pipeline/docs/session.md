# Session Guide — Secure Pipeline

## Learning Objectives

By the end of the session you can:

- make a build reproducible and explain what breaks it
- sign an artifact and its provenance with cosign and verify both
- check a provenance against an artifact and a commit
- make gates fail closed and deploy only what verifies

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Build it twice | 7 min |
| 0:17 | Act 2: Sign it | 7 min |
| 0:24 | Act 3: Say where it came from | 7 min |
| 0:31 | Act 4: Gates that fail closed | 7 min |
| 0:38 | Act 5: Deploy only what verifies | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and read the five parts in order. In the gates part, find the difference between the two lines about a missing tool.

## Exercises

1. Add a dependency to the library with a known critical flaw and see which gate stops it.
2. Make a build that embeds the date and fix it.
3. Add a fifth forged release whose provenance names an untrusted builder.
4. Write the same stages as a Woodpecker pipeline file.
