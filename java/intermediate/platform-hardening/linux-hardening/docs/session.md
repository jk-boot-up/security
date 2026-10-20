# Session Guide — Linux Hardening Checker

## Learning Objectives

By the end of the session you can:

- explain the set-user-id bit and why an unexpected one is critical
- say which permissions a secret file should have and why
- list the SSH and account settings that matter most
- explain why only permission fixes are automated

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: What the checker finds on a typical image | 8 min |
| 0:18 | Act 2: The fixes a machine can make | 8 min |
| 0:26 | Act 3: The decisions a person makes | 8 min |
| 0:34 | Act 4: Running it again | 8 min |
| 0:42 | Exercises | 18 min |

## Walkthrough

Run act one and ask which findings an attacker with a low-privilege shell would use first. In act two, ask why the checker does not delete the toor account. In act four, ask where the scan should run.

## Exercises

1. Add a rule for files with no owner (a user that has been deleted), using the `unix:uid` attribute.
2. Add a rule for `/etc/sudoers` entries with NOPASSWD, and decide if it is fixable.
3. Read the allow-lists from a policy file and add a test for an invalid file.
4. Make the scan fail a Gradle build when a CRITICAL finding is present.
