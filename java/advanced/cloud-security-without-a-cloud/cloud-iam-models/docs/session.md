# Session Guide — Cloud IAM Models

## Learning Objectives

By the end of the session you can:

- decide a request by hand with explicit deny, allow and implicit deny
- explain why two accounts must both agree
- describe what a boundary and a session policy can and cannot do
- explain the confused deputy and the external id

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Identity policies: default deny, allow, explicit deny | 7 min |
| 0:17 | Act 2: Accounts: who has to agree | 7 min |
| 0:24 | Act 3: Roles and the layers that narrow them | 7 min |
| 0:31 | Act 4: The confused deputy | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and find the case where one account's allow is not enough. In act three count the actions left at each layer. In act four find the line that makes the attack fail.

## Exercises

1. Add the condition operator DateLessThan and a policy that expires.
2. Add tag-based access control with aws:ResourceTag and test it.
3. Add iam:AttachRolePolicy to the escalation search and find a new path.
4. Write a policy for ana that allows only what the demo shows she uses.
