# Session Guide — Fuzzing: Finding the Inputs Nobody Tested

## Learning Objectives

By the end of the session you can:

- explain what a fuzzer finds that example tests do not
- write a harness that separates expected refusals from crashes
- explain why coverage guidance reaches deeper code than blind mutation
- shrink a crash to its smallest input and keep it as a regression test
- say what fuzzing cannot find

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The example tests pass | 7 min |
| 0:17 | Act 2: A mutation fuzzer | 7 min |
| 0:24 | Act 3: Guided by coverage | 7 min |
| 0:31 | Act 4: From crash to fix | 7 min |
| 0:38 | Act 5: What fuzzing does not find | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the new employee and the till. Run the demo one act at a time. After act two, ask what kind of input each crash needed. In act three, ask why matching one letter at a time
helps the fuzzer, and what a compiled string comparison does instead. In act five, ask which test would catch the quantity of -5.

## Exercises

1. Add a third parser, for a discount code such as SAVE-10, write its harness, and fuzz it.
2. Remove the letter-by-letter comparison in isNote and see how many runs the guided fuzzer needs now.
3. Add a dictionary mutation that inserts known field names, and compare the run at which the gift-message bug is found.
4. Turn the quantity of -5 into a property: every parsed quantity is between 1 and 99, and fuzz until it breaks.
