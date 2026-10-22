# Session Guide — Threat Modeling with pytm

## Learning Objectives

By the end of the session you can:

- describe a system for pytm
- read pytm's findings and their severities
- explain why an answer changes the findings
- name three things pytm does not do

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The bank, written for pytm | 7 min |
| 0:17 | Act 2: What pytm finds when nothing is said | 7 min |
| 0:24 | Act 3: Every answer changes the findings | 7 min |
| 0:31 | Act 4: A diagram and a report | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act three and find the stage that removes the most findings. In act four read the report template. In act five find what pytm cannot know.

## Exercises

1. Add the refund flow to the answers: encrypt it, and see which five findings go.
2. Add a template line for the mitigations pytm gives each finding.
3. Write a test that fails when an answer is true in the model and false in the code.
4. Add the plain project's decisions (owner, accept, review day) next to the findings.
