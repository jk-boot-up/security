# Session Guide — Threat Modeling with STRIDE

## Learning Objectives

By the end of the session you can:

- draw a system as elements, flows and trust boundaries
- apply STRIDE per element and explain each category
- rate a threat and choose between mitigate, accept, transfer and avoid
- turn a mitigation into a check that runs when the design changes

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The system as a data-flow model | 7 min |
| 0:17 | Act 2: STRIDE per element | 7 min |
| 0:24 | Act 3: Rating and deciding | 7 min |
| 0:31 | Act 4: Mitigations as tests | 7 min |
| 0:38 | Act 5: The bill, and the habit | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the architect's walk-through. Draw the bank on a whiteboard before running anything, and ask the group where the boundaries are. Run act one and compare. In act two, ask which threats the group missed. In act three, argue about one rating out loud. In act four, ask the group to propose a design change and predict which check fails.

## Exercises

1. Add a web shop next to the mobile app and find the new threats and checks.
2. Change one rating and see how the highest risks reorder.
3. Write a check for the accepted risk, then move it from accept to mitigate.
4. Add a review step that fails when an accepted risk is past its review day.
