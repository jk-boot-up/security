# Session Guide — Input Validation with Hibernate Validator

## Learning Objectives

By the end of the session you can:

- put constraints on a record and read the violations
- write a custom constraint and a class-level rule
- order checks with a group sequence
- name three things that validation does not do

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Where validation belongs | 7 min |
| 0:17 | Act 2: Say what is allowed, in annotations | 7 min |
| 0:24 | Act 3: Your own constraints, across fields, and in an order | 7 min |
| 0:31 | Act 4: Limits are validation too | 7 min |
| 0:38 | Act 5: The bill: what validation does not do | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act three and find which check asked the catalogue. In act four find the line that keeps the slow pattern away from long input. In act five find the template that was run as code.

## Exercises

1. Add a @Pattern to the e-mail so that it refuses ben@localhost.
2. Add a constraint that checks a postal code against a list of codes.
3. Move the catalogue lookup to a cheaper check and see what changes.
4. Write a message that never repeats the value, and test it with markup.
