# Session Guide — Secret Scanning

## Learning Objectives

By the end of the session you can:

- explain why a removed secret is still in the history
- compare a scanner with planted secrets
- use an allowlist and a baseline
- block a secret with a client hook and a server hook

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The history is the repository | 7 min |
| 0:17 | Act 2: What each one gets wrong | 7 min |
| 0:24 | Act 3: An allowlist and a baseline | 7 min |
| 0:31 | Act 4: Blocking the next one | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and compare the first two scans. In the second act find the secret that gitleaks misses and the look-alike that both flag.

## Exercises

1. Add a rule to the small scanner for database URLs with a password and measure it.
2. Plant a secret in a binary file and see which scanner finds it.
3. Add a gitleaks:allow comment to the test key and see what it does.
4. Make the pre-receive hook scan only the new commits of an existing branch.
