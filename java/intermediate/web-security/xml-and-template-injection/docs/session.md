# Session Guide — XML External Entities and Template Injection

## Learning Objectives

By the end of the session you can:

- explain what a DOCTYPE and an external entity are, and what the parser does with them
- list the parser settings that stop XXE and say why refusing the DOCTYPE comes first
- explain why customer text must never become part of a template
- say what makes a template engine logic-less and why that helps

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: An invoice upload | 7 min |
| 0:17 | Act 2: The parser reads what the file tells it to | 7 min |
| 0:24 | Act 3: Refuse the DOCTYPE | 7 min |
| 0:31 | Act 4: A template engine that evaluates expressions | 7 min |
| 0:38 | Act 5: A logic-less engine keeps data as data | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and ask whose instruction the parser followed. In act four, ask which of the two things in the pasted line was code and which was data. In act five, ask what the engine would need
to be able to do before the same attack worked again.

## Exercises

1. Add a second field that is read from an attribute, and check that the safe parser handles it.
2. Switch off the DOCTYPE refusal in the safe parser and see which of the other settings still stop the file entity.
3. Add `{{order_number}}` to the logic-less engine, with a test that its value is escaped.
4. Write the template as a file in resources and load it at start-up, so no code path builds one from input.
