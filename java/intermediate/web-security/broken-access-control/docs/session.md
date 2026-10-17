# Session Guide — Broken Access Control

## Learning Objectives

By the end of the session you can:

- explain IDOR, and why an unguessable id does not fix it
- tell horizontal from vertical privilege escalation
- say why a check in the page is not a check
- design access control as one policy at one choke point, deny by default
- keep the policy complete with a test

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Walking order numbers | 7 min |
| 0:17 | Act 2: Routes that nobody checks | 7 min |
| 0:24 | Act 3: Sideways and upwards | 7 min |
| 0:31 | Act 4: One choke point, deny by default | 7 min |
| 0:38 | Act 5: Keeping it right, and the bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the hotel key cards. Run the demo act by act. After act one, ask what ana needed to know to read ben's order. After act two, ask how ana found the refund route, and lead to the
browser's developer tools. Before act four, ask the group to design the fix. After act five, ask what else the route inventory test could check.

## Exercises

1. Add a support-only route for refund notes, and write its rule first, then its handler.
2. Change someone else's order to answer 403 instead of 404, and discuss what the support team gains and what an attacker gains.
3. Make the ownership check part of the lookup itself, so that a handler can only ever load the caller's own orders.
4. Extend the route inventory test so that it also fails when a rule exists for a route that no longer exists.
