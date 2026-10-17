# Session Guide — HTTP Basics and Security Headers with nginx

## Learning Objectives

By the end of the session you can:

- explain what the word always changes in add_header
- explain the inheritance rule and how to audit for it
- name two requests nginx refuses because they are ambiguous
- say why hiding a version number is not a defence

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: nginx in front of the shop, as it comes | 7 min |
| 0:17 | Act 2: add_header, and the word always | 7 min |
| 0:24 | Act 3: The inheritance trap | 7 min |
| 0:31 | Act 4: Limits, and requests that are ambiguous | 7 min |
| 0:38 | Act 5: Hiding what the application gives away, and the bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the doorman. Run the demo one act at a time. After act two, ask what an attacker learns from a bare 404 page. After act three, ask how the team would find a location that lost its headers. In act four, ask why nginx must agree with the application about where a request ends.

## Exercises

1. Add an `include` of a shared snippet file instead of repeating the headers, and check it with the auditor.
2. Add a `/static` location with `expires` and see which headers it keeps.
3. Make the Content-Security-Policy weaker and watch the score drop.
4. Send a request with a space in a header name and see what status nginx gives.
