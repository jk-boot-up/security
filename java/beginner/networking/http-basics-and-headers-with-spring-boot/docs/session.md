# Session Guide — HTTP Basics and Security Headers with Spring Boot

## Learning Objectives

By the end of the session you can:

- name the five security headers the audit checks
- explain why Strict-Transport-Security is sent only over HTTPS
- harden a session cookie with properties
- explain what a forwarded header is and why trusting it is a decision

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: The same page, with and without header writers | 7 min |
| 0:17 | Act 2: The same page behind a proxy that ends HTTPS | 7 min |
| 0:24 | Act 3: The cookie that carries the cart's session | 7 min |
| 0:31 | Act 4: A line break inside a header | 7 min |
| 0:38 | Act 5: Limits on the size of a request | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the standard locks. Run the demo one act at a time. After act one, ask why the framework leaves out one header. After act two, ask what an attacker could do if the store were reachable without the proxy. In act four, ask what the code would have to do
if the container did not refuse the line break.

## Exercises

1. Remove the `forward-headers-strategy` property and check that act two drops to 4 of 5.
2. Change the Content-Security-Policy to allow 'unsafe-inline' and watch the audit fail.
3. Raise the header limit until the 4000-character header is accepted by the hardened store.
4. Add a `Permissions-Policy` header and extend the audit to six headers.
