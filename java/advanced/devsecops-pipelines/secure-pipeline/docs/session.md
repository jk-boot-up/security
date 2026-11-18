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
| 0:10 | Act 1: Build it twice | 5 min |
| 0:15 | Act 2: Sign it | 5 min |
| 0:20 | Act 3: Say where it came from | 5 min |
| 0:25 | Act 4: Four gates, six planted flaws | 5 min |
| 0:30 | Act 5: A gate whose tool cannot answer | 5 min |
| 0:35 | Act 6: Deploy only what verifies | 5 min |
| 0:40 | Exercises | 20 min |

## Walkthrough

Run the demo and read the six parts in order. In part five, find the difference between the two pipelines on the same broken tool.

## Exercises

1. Add a dependency to the library with a known critical flaw and see which gate stops it.
2. Make a build that embeds the date and fix it.
3. Add a fifth forged release whose provenance names an untrusted builder.
4. Write the same stages as a Woodpecker pipeline file.
