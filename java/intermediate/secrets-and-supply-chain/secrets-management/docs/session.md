# Session Guide — Secrets Management

## Learning Objectives

By the end of the session you can:

- name three places secrets leak outside the source code
- explain why run-time injection from a file is better than an environment variable
- explain what a lease is and why short-lived, per-application logins shrink a leak
- describe a rotation that never leaves the application without a working key

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Where secrets leak | 7 min |
| 0:17 | Act 2: Secrets injected at run time | 7 min |
| 0:24 | Act 3: A secrets service that hands out leased logins | 7 min |
| 0:31 | Act 4: Rotation without downtime, and the blast radius of a leak | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the hotel and its key cards. Run the demo one act at a time. After act one, ask where else in the group's own systems a secret might
sit in an image or an environment. After act three, ask what happens to the shop if the secrets service is down. After act four, compare the two
blast radii, and ask what the static password's would be in the group's own systems.

## Exercises

1. Add a scanner rule for private-key blocks (a BEGIN PRIVATE KEY line) and test it.
2. Give the secrets service a policy so that the reports job may only lease read-only logins.
3. Make the shop renew its lease automatically at two thirds of its time to live, and test it with the simulated clock.
4. Rotate the database login every hour while a simulated request stream keeps working.
