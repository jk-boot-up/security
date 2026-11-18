# Session Guide — Cross-Site Scripting with Spring Boot: Thymeleaf Escaping and a Nonce CSP

## Learning Objectives

By the end of the session you can:

- say what th:text escapes and what th:utext does not
- write a value safely into an attribute, a link and a script with Thymeleaf
- add a per-request nonce to Spring Security's policy
- write a test that stops new uses of th:utext

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Thymeleaf escapes by default | 8 min |
| 0:18 | Act 2: th:utext, the one switch that turns escaping off | 8 min |
| 0:26 | Act 3: The other places a value can go | 8 min |
| 0:34 | Act 4: A strict Content-Security-Policy from Spring Security | 8 min |
| 0:42 | Exercises | 18 min |

## Walkthrough

Start with the printer and its one switch. Run the demo. After act two, ask where else in a real code base markup could be built outside the template. After act four, ask what happens
if a developer adds an inline script without the nonce, and run the guard test to see it fail.

## Exercises

1. Add a second template that uses th:utext and watch the guard test fail.
2. Add an inline script to product.html without the nonce and check what the policy evaluator says.
3. Move the fit note into an onclick attribute and explain why escaping is no longer enough.
4. Allow a formatted review safely by converting a few known tags on the server and escaping everything else.
