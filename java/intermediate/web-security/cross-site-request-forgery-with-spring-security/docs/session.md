# Session Guide — Cross-Site Request Forgery with Spring Security

## Learning Objectives

By the end of the session you can:

- explain what Spring Security's default token protects and how it is checked
- choose the cookie mode for a single-page app and name its price
- name the ways the protection is lost: disabling, exempting and GET
- decide when a chain can safely leave CSRF off

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: CSRF protection switched off | 7 min |
| 0:17 | Act 2: Spring Security's default token | 7 min |
| 0:24 | Act 3: A single-page app | 7 min |
| 0:31 | Act 4: What the default does not cover | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act one and ask why the browser sent the cookie. In act three ask what an XSS hole would do. In act four ask which of the three losses would show up in a code review.

## Exercises

1. Add a Fetch Metadata filter that refuses a cross-site post, and test it beside the token.
2. Write a test that fails if any route in the web chain is on the exemption list without a signature check.
3. Add the `__Host-` prefix to the session cookie and see what the server requires.
4. Move the token from a header to a form field for a server-rendered page.
