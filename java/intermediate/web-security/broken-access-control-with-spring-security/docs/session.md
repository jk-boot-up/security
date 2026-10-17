# Session Guide — Broken Access Control with Spring Security

## Learning Objectives

By the end of the session you can:

- read a SecurityFilterChain and find the rule a request matches
- explain why deny by default must be the last rule
- put ownership into the query rather than after it
- use method security for rules that must hold for every caller
- keep URL rules complete with a route inventory test

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: A Spring store with three common mistakes | 7 min |
| 0:17 | Act 2: Deny by default in the filter chain | 7 min |
| 0:24 | Act 3: The object-level check, in the query | 7 min |
| 0:31 | Act 4: Method security, and a log of every refusal | 7 min |
| 0:38 | Act 5: The route inventory, and the bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the building and its locks. Show the naive configuration and ask the group which requests it lets through before running act one. After act two, ask what the filter chain cannot know.
After act three, compare the 404 and the 403 and ask what each tells an attacker. Finish by adding a route without a rule and running the tests.

## Exercises

1. Add a support-only route for refund notes: write its rule first, watch the inventory test pass, then remove the rule and watch it fail.
2. Move the staff check out of OrderAccess into a @PreAuthorize expression and compare the two.
3. Make the route inventory test also fail when a rule exists for a route that no longer exists.
4. Replace HTTP Basic with a bearer token and check that every status stays the same.
