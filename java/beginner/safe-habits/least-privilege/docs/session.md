# Session Guide — The Principle of Least Privilege

## Learning Objectives

By the end of the session you can:

- explain least privilege and blast radius
- describe deny by default and why an explicit deny wins
- give an example of separation of duties
- explain why access should expire
- describe how to find unused permissions

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Roles and permissions, and deny by default | 7 min |
| 0:17 | Act 2: Blast radius: what a stolen credential can reach | 7 min |
| 0:24 | Act 3: Separation of duties: two people for a refund | 7 min |
| 0:31 | Act 4: Access that expires by itself | 7 min |
| 0:38 | Act 5: Privilege creep, and the operating system | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the hotel keys. Run the demo one act at a time. After act two, ask the group which of their own accounts or jobs are admin because it was quick. After act three, ask where in their work one person can do something risky from start to finish.
After act five, ask who removes a permission once it is found to be unused, and who notices.

## Exercises

1. Add a new role, `refund-reviewer`, and check how many of the 15 permissions it reaches.
2. Make the grant renewable once, and decide who has to approve the renewal.
3. Add a rule that nobody may both create and approve any permission change.
4. Change the audit limit from 90 days to 30 and see which permissions are flagged.
