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
| 0:10 | Act 1: Spring Security present, everything permitted | 5 min |
| 0:15 | Act 2: Authentication: HTTP Basic and BCrypt | 5 min |
| 0:20 | Act 3: The login on the wire: the HTTP Basic header | 5 min |
| 0:25 | Act 4: The password check, step by step: BCrypt | 5 min |
| 0:30 | Act 5: Authenticated is not the same as authorized | 5 min |
| 0:35 | Act 6: Authorization: @PreAuthorize rules on the endpoints | 5 min |
| 0:40 | Act 7: The bill: a forgotten rule, a check every time | 5 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run the plain project's demo first, then this one, and compare the acts line by line: the requests and their
responses are the same. Acts three and four go further, and take the Basic header and the BCrypt check apart. Then open the configuration classes side by side, `LoginOnlyConfig` and `PolicyConfig`, and find the one
line that differs. Open `ShopController` and read each `@PreAuthorize` sentence aloud. In act seven, ask the group where
else a rule could be forgotten, and what a test for that would look like.

## Exercises

1. Add an endpoint `POST /orders/{id}/note` that only the owner may use, and write the test first.
2. Change the BCrypt cost from 4 to 12 and time the demo. What changes?
3. Switch the second layer to deny by default: `anyRequest().denyAll()` at the end of the rules. Which endpoints break?
4. Replace `hasRole('ADMIN')` on export with `hasAuthority('EXPORT')` and give Sam that authority.
