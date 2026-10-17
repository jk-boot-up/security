# Session Guide — Input Validation Done Right

## Learning Objectives

By the end of the session you can:

- explain why checks in the page are not validation
- write a value type that parses instead of checking
- canonicalise text before comparing or checking it
- spot a regular expression that can take exponential time
- say what validation leaves to encoding, parameters and access checks

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Where validation belongs | 7 min |
| 0:17 | Act 2: Say what is allowed, and parse into types | 7 min |
| 0:24 | Act 3: Canonicalise before validating | 7 min |
| 0:31 | Act 4: Limits are validation too | 7 min |
| 0:38 | Act 5: The bill: what validation does not do | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the concert tickets. Run the demo one act at a time. After act one, ask who wrote the hidden price. In act three, ask the group to spell ana in as many ways as they can. In act four,
ask how many characters would take a minute, and lead to why a length limit comes first.

## Exercises

1. Add a postcode type whose rule depends on the country, and test it for two countries.
2. Add a limit on the number of fields in a request and test a form with a thousand fields.
3. Find a nested repetition in a real pattern you use, and rewrite it.
4. Extend the look-alike table with three Greek letters and test that a staff name spelled with them is refused.
