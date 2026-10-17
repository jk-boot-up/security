# Session Guide — HTTP Basics and Security Headers

## Learning Objectives

By the end of the session you can:

- describe the parts of an HTTP/1.1 request and response
- explain how two programs reading one message differently allows smuggling
- explain header injection and how to prevent it
- choose cookie attributes for a login session
- name five security headers and what each tells the browser

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: An HTTP message is just text | 7 min |
| 0:17 | Act 2: Messages that two programs could read differently | 7 min |
| 0:24 | Act 3: Header injection | 7 min |
| 0:31 | Act 4: Cookies that carry a login | 7 min |
| 0:38 | Act 5: Security headers on a real response | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the order form analogy. Run the demo one act at a time. After act two, ask what a proxy would do with the hidden request. After act three, ask where else customer text reaches a header. In act four, ask which attribute stops which attack.
In act five, ask why presence is not enough.

## Exercises

1. Add support for a chunked body, keep the refusal of Content-Length together with Transfer-Encoding, and test it.
2. Add a sixth header, Permissions-Policy, to the auditor.
3. Add the Max-Age and Expires attributes to the cookie judge and flag a session cookie that lasts a year.
4. Run `curl -v` against the loopback server and match each line to the parser's rules.
