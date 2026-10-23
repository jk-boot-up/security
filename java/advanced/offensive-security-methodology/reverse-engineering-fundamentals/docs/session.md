# Session Guide — Reverse Engineering Fundamentals

## Learning Objectives

By the end of the session you can:

- read strings and names from a class file without running it
- recognise a comparison in javap output
- patch a method with the JDK class file API
- explain what obfuscation stops and does not stop

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The program | 7 min |
| 0:17 | Act 2: Reading it without running it | 7 min |
| 0:24 | Act 3: Decompiling it | 7 min |
| 0:31 | Act 4: Changing it | 7 min |
| 0:38 | Act 5: Obfuscation | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the demo and follow the order of cost: strings, names, bytecode, decompiler. In the last act find the three things that obfuscation leaves.

## Exercises

1. Move the licence check to a small server and see what an attacker can still change.
2. Add a check that the class files have not been changed and patch that too.
3. Write a rule that finds hard-coded keys in the constant pools of a jar.
4. Rename the methods with the class file API and see what javap shows.
