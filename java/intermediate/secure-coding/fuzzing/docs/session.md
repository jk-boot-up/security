# Session Guide — Fuzzing a Parser

## Learning Objectives

By the end of the session you can:

- state the oracle for a parser, and why 'only ParseException' is the right one
- explain why random text finds little and what coverage feedback adds
- describe what a minimizer does and why it matters
- turn a finding into a regression test

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The parser works on the files it was written for | 8 min |
| 0:18 | Act 2: Three ways to make inputs, 5000 runs each | 8 min |
| 0:26 | Act 3: Each bug, as the smallest input that still triggers it | 8 min |
| 0:34 | Act 4: After the fix | 8 min |
| 0:42 | Exercises | 18 min |

## Walkthrough

Run act two and ask why random text covers one branch. In act three, read each minimized input and ask which line of the parser it reaches. In act four, ask why finding nothing is weaker than it sounds.

## Exercises

1. Add a sixth bug to the parser and see which strategy finds it first.
2. Add an oracle that checks the total of each line against a slow, obviously correct calculation.
3. Run with another seed and compare the run numbers where the bugs are found.
4. Add a crossover mutation that joins halves of two corpus inputs.
