# Session Guide — Dependency Confusion

## Learning Objectives

By the end of the session you can:

- explain how a public package can replace a private one
- say which tool behaviours are vulnerable and why
- describe content filtering by name and why it fails closed
- explain what a lockfile digest adds to a pinned version

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A build that works | 7 min |
| 0:17 | Act 2: Somebody registers the online store's package name in the public registry | 7 min |
| 0:24 | Act 3: Repository order helps only until a name is missing | 7 min |
| 0:31 | Act 4: Names decide where a package may come from | 7 min |
| 0:38 | Act 5: Locked digests, and watching the names | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and ask what the attacker needed to know and what they needed to own. In act three, ask what a pin protects and what it does not. In act five, ask who reviews a changed digest.

## Exercises

1. Add a second internal prefix and update the filter, with a test that an old name is still routed correctly.
2. Add typosquatting detection: report public names within one edit of an internal name.
3. Make the build refuse install scripts from public packages unless they are on an allow-list.
4. Write the check that fails CI when a lockfile entry changes without a matching review note.
