# Session Guide — Runtime Security

## Learning Objectives

By the end of the session you can:

- write a Falco rule for a shell, a write and a connection
- explain why a rule needs a baseline
- explain why Falco's driver needs BTF or a kernel module
- explain the difference between preventing and detecting

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Falco on this machine | 5 min |
| 0:15 | Act 2: The service doing its work | 5 min |
| 0:20 | Act 3: A shell in the container | 5 min |
| 0:25 | Act 4: A write where it never writes | 5 min |
| 0:30 | Act 5: A connection it never makes | 5 min |
| 0:35 | Act 6: Prevent and detect | 5 min |
| 0:40 | Exercises | 20 min |

## Walkthrough

Run the demo and compare the second act with the next three. Then find in the last act which part refused and which part told you.

## Exercises

1. Add a rule for a program that is written and then run (drop and execute).
2. Add the ledger to the baseline of a second service and see what changes.
3. Make the sensor read the container from the kernel record instead of afterwards, and find what the audit record lacks.
4. Run the rules file in Falco on a Linux machine with BTF.
