# Session Guide — CORS with Spring Boot

## Learning Objectives

By the end of the session you can:

- explain what CORS lets a page read and what it does not stop
- write an exact CorsConfiguration for a credentialed API
- name the policies that look like fixes and what each one gives away
- register the policy where the preflight can reach it

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: No policy: a page may send, but not read | 7 min |
| 0:17 | Act 2: Policies that look like fixes | 7 min |
| 0:24 | Act 3: An exact allow-list | 7 min |
| 0:31 | Act 4: Where the policy is registered | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act one and ask what the browser protected. In act two ask which of the three workarounds is the most common in your own code. In act four ask why a preflight cannot carry a login.

## Exercises

1. Add a second partner and a test for it, its lookalike and its null origin.
2. Move the policy from the security configuration to a CorsFilter bean and compare the order of the filters.
3. Add a custom request header to the PUT and see what the preflight needs to allow.
4. Make the allow-list come from a property and fail startup if it contains a star.
