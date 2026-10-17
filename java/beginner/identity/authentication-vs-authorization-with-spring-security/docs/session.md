# Session Guide — Authentication vs Authorization with Spring Security

## Learning Objectives

By the end of the session you can:

- name the parts of Spring Security that answer who is asking, and what they may do
- read a `@PreAuthorize` rule and say who it allows
- explain why a forgotten rule leaves an endpoint open, and how a second layer helps
- explain why HTTP Basic costs a password check on every request

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Spring Security present, but every request permitted | 7 min |
| 0:17 | Act 2: Authentication: HTTP Basic, passwords stored as BCrypt | 7 min |
| 0:24 | Act 3: Authenticated is not the same as authorized | 7 min |
| 0:31 | Act 4: Authorization: @PreAuthorize rules on the endpoints | 7 min |
| 0:38 | Act 5: The bill: a forgotten rule, and a password check on every request | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the plain project's demo first, then this one, and compare the five acts line by line: they print the same
results. Then open the configuration classes side by side, `LoginOnlyConfig` and `PolicyConfig`, and find the one
line that differs. Open `ShopController` and read each `@PreAuthorize` sentence aloud. In act five, ask the group where
else a rule could be forgotten, and what a test for that would look like.

## Exercises

1. Add an endpoint `POST /orders/{id}/note` that only the owner may use, and write the test first.
2. Change the BCrypt cost from 4 to 12 and time the demo. What changes?
3. Switch the second layer to deny by default: `anyRequest().denyAll()` at the end of the rules. Which endpoints break?
4. Replace `hasRole('ADMIN')` on export with `hasAuthority('EXPORT')` and give Sam that authority.
