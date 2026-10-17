# Session Guide — An HTTPS Server, Put Together

## Learning Objectives

By the end of the session you can:

- list what a secure web server needs besides a certificate
- explain what HSTS does and what it cannot do
- explain what the Secure flag does for a cookie
- explain why a client should demand TLS 1.3 when it can

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: An HTTPS request that is checked | 7 min |
| 0:17 | Act 2: Plain HTTP only redirects | 7 min |
| 0:24 | Act 3: A cookie that stays on HTTPS | 7 min |
| 0:31 | Act 4: What the client refuses | 7 min |
| 0:38 | Act 5: Scoring a shop from outside | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the shop and its doors. Run the demo one act at a time. After act two, ask what the first visit of a new customer looks like. After act three, ask how a cookie can leak on a site that uses HTTPS everywhere.
After act four, ask which refusal protects against which attacker. Finish with the checklist and ask what a sixth item would be.

## Exercises

1. Add a sixth check to the checklist: the server does not offer the weak version of TLS. Add the matching server profile.
2. Make the strong shop send HSTS with a short max-age and see which check fails.
3. Add a `/logout` route that expires the cookie, with the same attributes, and test it.
4. Add a certificate that has expired to the keystore and show the client refusing it.
