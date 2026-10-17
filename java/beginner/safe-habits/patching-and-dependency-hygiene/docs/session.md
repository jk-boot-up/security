# Session Guide — Patching and Dependency Hygiene

## Learning Objectives

By the end of the session you can:

- explain what a software bill of materials is for
- say why versions must be compared as numbers
- tell a direct dependency from a transitive one, and how each is fixed
- explain why a CVSS score is not a priority by itself
- say what a pinned hash protects against, and what it does not

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: An inventory of what the store ships | 7 min |
| 0:17 | Act 2: Matching the inventory against known problems | 7 min |
| 0:24 | Act 3: How bad, and what to fix first | 7 min |
| 0:31 | Act 4: Patch windows | 7 min |
| 0:38 | Act 5: Pinning, and checking what you download | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the restaurant and the recall. Run the demo one act at a time. After act one, ask the group which dependencies they have never heard of in their own projects. After act three, ask what the store would lose by fixing in score order alone.
In act five, ask what a pin cannot tell you: that the reviewed artifact was good in the first place.

## Exercises

1. Add a new advisory for acme-crypto that affects 2.0.5 and check that it appears, with its path.
2. Make the store use xml-entities and see how the order of the findings changes.
3. Change the day of today to 100 and list which findings breach their deadline.
4. Add a second route to acme-json through acme-image and check that the shortest path is reported.
5. Write a lookalike-name check that also catches two swapped letters.
