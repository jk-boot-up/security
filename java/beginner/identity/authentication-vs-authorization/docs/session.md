# Session Guide — Authentication vs Authorization

## Learning Objectives

By the end of the session you can:

- explain the difference between authentication and authorization in your own words
- say when a system should answer 401 and when 403
- name the mistake of checking only that a user is logged in
- explain what a stale role is and one way to avoid it

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The first version, with no checks | 7 min |
| 0:17 | Act 2: Authentication: finding out who is asking | 7 min |
| 0:24 | Act 3: Authenticated is not the same as authorized | 7 min |
| 0:31 | Act 4: Authorization: what may this person do? | 7 min |
| 0:38 | Act 5: The bill: sessions expire, and roles go stale | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the hotel analogy: passport at the desk, key card at the door. Run the
demo one act at a time. After act two, ask the group what a request with a
made-up token should get, before reading the answer. After act three, ask what
the bug is, and who it hurts. In act five, ask the group whether they would
rather have a fast gate with a stale role or a current gate with a lookup, and
for what kind of shop.

## Exercises

1. Make support unable to refund more than the order's amount of 100 dollars. Which class changes, and which test is added?
2. Add a LOGOUT step to act five and show that the token then gets 401.
3. Change the session lifetime to 1 tick. Which acts of the demo change?
4. Add a rule that an admin may not refund their own order, and write the test first.
