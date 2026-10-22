# Session Guide — Linux Hardening with Docker

## Learning Objectives

By the end of the session you can:

- scan a container from the inside
- separate what a machine can fix from what a person decides
- say why a fix belongs in the image
- name two things a container cannot change

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: What the scan finds in a typical image | 7 min |
| 0:17 | Act 2: The fixes a machine can make, and where they live | 7 min |
| 0:24 | Act 3: The decisions a person makes, in the recipe | 7 min |
| 0:31 | Act 4: Running it again | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and find the line that shows the fixes disappear. In act three list the decisions that were made. In act five find the setting that is not the image's to change.

## Exercises

1. Add a rule for files owned by a user that does not exist.
2. Add a USER line to the typical image and see which findings change.
3. Scan a public image such as nginx with the same script.
4. Make the scan a step in a build that fails on a critical finding.
