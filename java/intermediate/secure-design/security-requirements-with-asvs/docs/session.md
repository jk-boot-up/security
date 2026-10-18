# Session Guide — Security Requirements with OWASP ASVS

## Learning Objectives

By the end of the session you can:

- explain what the three ASVS levels are and choose one for an application
- write a security requirement that can be verified, and say how
- turn a requirement into an automated or configuration check
- read a coverage report and explain what met, failed, not applicable and manual mean

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Choosing a level | 7 min |
| 0:17 | Act 2: Requirements written down as data | 7 min |
| 0:24 | Act 3: Checks that ask the running store | 7 min |
| 0:31 | Act 4: Coverage and gaps | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the building inspector's checklist. Ask the group which level the store and the bank need before running act one. In act two, ask the group to rewrite one statement so it can be tested. In act three, ask which checks a code review would have caught. In act four, introduce another regression and predict which requirement fails.

## Exercises

1. Add a requirement from the Authorization chapter and write a check for it against the store.
2. Change the store's profile so it moves money, and see which requirements now apply.
3. Introduce a regression of your own, such as dropping HttpOnly, and watch the release gate refuse it.
4. Map SR-14 to the current edition and add a test that fails when any requirement names an old edition.
