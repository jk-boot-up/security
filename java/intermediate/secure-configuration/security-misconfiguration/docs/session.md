# Session Guide — Security Misconfiguration

## Learning Objectives

By the end of the session you can:

- name the common misconfigurations a scanner tries first
- explain secure defaults and why every setting should be stated
- describe a startup gate and a drift check, and when each runs
- explain why a passing checker is not proof of security

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The store started with its shipped defaults | 7 min |
| 0:17 | Act 2: Secure defaults, and a gate at startup | 7 min |
| 0:24 | Act 3: Drift: development settings that reach production | 7 min |
| 0:31 | Act 4: Banners, methods and file permissions | 7 min |
| 0:38 | Act 5: The bill: a baseline that must be checked again and again | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the spare key in the door. Run the demo one act at a time. After act one, ask which finding an attacker would try first. After act two, ask what the team on call needs when the
gate refuses to start. In act three, ask how production files are made in the group's own projects. In act five, ask which settings the checker does not cover.

## Exercises

1. Add a check for a missing `Content-Security-Policy` header and make the hardened store pass it.
2. Add an `allowed.origins` setting with a wildcard default and teach the gate to refuse it in production.
3. Make the checker fail when the Server header names the product even without a version.
4. Write the drift check as a test that fails the build when production differs from the baseline.
