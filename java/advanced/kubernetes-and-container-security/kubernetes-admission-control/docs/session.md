# Session Guide — Kubernetes Admission Control

## Learning Objectives

By the end of the session you can:

- explain what an admission controller sees and what it does not
- write a validating, a mutating and an image policy
- roll a rule out in audit mode before it denies
- explain why the policy engine itself must stay available

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Policies that refuse | 7 min |
| 0:17 | Act 2: Audit first, then deny | 7 min |
| 0:24 | Act 3: Mutation and signed images | 7 min |
| 0:31 | Act 4: Exceptions | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and find the four admitted requests in the first act. In the last act find what Kyverno does when it stops.

## Exercises

1. Add a policy that refuses a container with no readiness probe, first in audit mode.
2. Make the signed-images policy accept two keys.
3. Add a second replica of the admission controller and repeat the last act.
4. Write an exception that ends tomorrow and watch it stop working.
