# Session Guide — CORS Done Right

## Learning Objectives

By the end of the session you can:

- explain what the same-origin policy blocks and what it does not
- say what a simple request is and when a preflight happens
- list the unsafe CORS policies and the attacker each lets in
- write an exact allow-list and explain Vary: Origin

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Same origin needs no CORS | 7 min |
| 0:17 | Act 2: No CORS headers: a page may send, but not read | 7 min |
| 0:24 | Act 3: Policies that look like fixes | 7 min |
| 0:31 | Act 4: An exact allow-list | 7 min |
| 0:38 | Act 5: Wildcard and cookies never combine | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and ask which of the requests CORS could have stopped. In act three, ask for each policy what a test with the partner's page would show. In act five, ask why teams end up reflecting the origin.

## Exercises

1. Add a second partner and a test that the look-alike of each is blocked.
2. Add `Access-Control-Expose-Headers` for one header and check that others stay hidden.
3. Change the session cookie to SameSite=Lax in the model and see which credentialed calls stop working.
4. Remove `Vary: Origin` and describe how a shared cache could leak an answer.
