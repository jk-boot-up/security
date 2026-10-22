# Session Guide — TLS Servers in Practice with nginx and Caddy

## Learning Objectives

By the end of the session you can:

- read an nginx TLS configuration line by line
- say what a client sees for each setting
- find the add_header trap in a configuration
- plan renewal for each server

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: nginx with a certificate from the local authority | 7 min |
| 0:17 | Act 2: Which protocol versions it speaks | 7 min |
| 0:24 | Act 3: HSTS, and the add_header trap | 7 min |
| 0:31 | Act 4: Caddy makes its own certificates | 7 min |
| 0:38 | Act 5: The bill | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Run act two and find the line that changes the protocol. In act three ask which page lost HSTS and why. In act five ask what a renewal job for nginx must end with.

## Exercises

1. Add ssl_ciphers and see what a TLS 1.2 client negotiates.
2. Add Strict-Transport-Security with preload and read what that promises.
3. Put Caddy in front of nginx and decide which one does TLS.
4. Use the certificates from the step-ca project in nginx.
