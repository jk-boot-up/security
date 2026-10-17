# Session Guide — An HTTPS Server with Spring Boot

## Learning Objectives

By the end of the session you can:

- say which property turns on TLS and which turns on client certificates
- explain why a client must check the host name as well as the certificate
- explain why the plain port's redirect carries no HSTS header
- describe how a certificate is renewed without a restart
- say what mutual TLS proves and what it does not

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: TLS from an SSL bundle | 7 min |
| 0:17 | Act 2: Redirect, HSTS and secure cookies | 7 min |
| 0:24 | Act 3: Renewing a certificate without a restart | 7 min |
| 0:31 | Act 4: Mutual TLS: the server checks the client too | 7 min |
| 0:38 | Act 5: What Spring leaves to you | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the standard door. Run the demo one act at a time. After act one, ask which refusal protects against which attacker. After act two, ask what happens on a customer's very first visit, before HSTS has been seen.
In act three, ask what happens if nobody renews. In act four, ask what Mallory would need to get in. In act five, ask which property would silently undo each of the earlier acts if it were misspelt.

## Exercises

1. Allow TLS 1.2 as well, and see which refusal in act one changes.
2. Misspell `server.ssl.bundle` and find out what the server does, then add a test that would have caught it.
3. Turn off reload and check that the old certificate keeps being served after the file changes.
4. Add a third client bundle whose certificate has the wrong key usage and see what the server says.
