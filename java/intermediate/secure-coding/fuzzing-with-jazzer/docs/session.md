# Session Guide — Fuzzing with Jazzer

## Learning Objectives

By the end of the session you can:

- write a fuzz target for a parser
- choose seed inputs
- make a crash input smallest and keep it as a regression test
- name three things a fuzzer does not find

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The example tests pass | 7 min |
| 0:17 | Act 2: The real Jazzer | 7 min |
| 0:24 | Act 3: Guided by coverage, and by what you give it | 7 min |
| 0:31 | Act 4: From crash to fix | 7 min |
| 0:38 | Act 5: What fuzzing finds besides crashes, and what it does not | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act three and compare the coverage with and without seeds. In act four read the smallest input of each crash. In act five find the finding that is not a crash.

## Exercises

1. Write a fuzz target for the e-mail address check of the validation project.
2. Add a property to a fuzz target: parse, print and parse again gives the same value.
3. Run the @FuzzTest tests with JAZZER_FUZZ=1 for a minute and see what is saved.
4. Give the cart target a larger input length and find the nesting crash.
