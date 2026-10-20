# Session Guide — SQL Injection and Its Defences

## Learning Objectives

By the end of the session you can:

- explain how a quote changes the meaning of a pasted statement
- describe why a parameter is never parsed as SQL
- say why column names need an allow-list and not a parameter
- explain what least privilege adds when the query is already fixed

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A search that works | 8 min |
| 0:18 | Act 2: A quote turns text into code | 8 min |
| 0:26 | Act 3: Parameters keep text as text | 8 min |
| 0:34 | Act 4: Few rights limit the damage | 8 min |
| 0:42 | Exercises | 18 min |

## Walkthrough

Run act two and ask which part of the text the customer wrote and which part the program wrote. Run act three and ask what changed in the statement,
not in the input. In act four, ask what an attacker would need next if the users table were readable.

## Exercises

1. Add a filter by maximum price and keep the statement fixed.
2. Escape `%` and `_` in the search words so that the customer's text is matched literally.
3. Add a table the search user may not read, and write a test that a union with it is refused.
4. Make the engine's error messages generic, and see what an attacker learns from the original ones.
