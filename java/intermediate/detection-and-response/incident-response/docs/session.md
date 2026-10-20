# Session Guide — Incident Response: Prepare, Contain, Eradicate, Recover, Learn

## Learning Objectives

By the end of the session you can:

- name the phases of incident handling and the evidence each needs
- explain why isolating is better than rebooting during an incident
- list the ways back in an attacker leaves, and how to find them
- say why a rebuild from a known-good image beats cleaning in place

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The attack | 5 min |
| 0:15 | Act 2: What was prepared | 5 min |
| 0:20 | Act 3: Response A: nothing prepared, and a panicked fix | 5 min |
| 0:25 | Act 4: Response B: an alert, but no plan | 5 min |
| 0:30 | Act 5: Response C: the playbook | 5 min |
| 0:35 | Act 6: The review | 5 min |
| 0:40 | Exercises | 20 min |

## Walkthrough

Run the three responses and, for each action, ask what the responder knew when they took it. In the review, find the first decision that separates B from C. Then ask which preparation would have shortened the 15,000 rows.

## Exercises

1. Add a rate limit on the database account in the preparation, and see how many rows it saves.
2. Add a tabletop step where the team decides who tells customers and regulators, and when the clock starts.
3. Add the restore of a backup and decide which backup is safe given the scope.
4. Add a fourth foothold the logs do not show, and see what the monitoring period catches.
