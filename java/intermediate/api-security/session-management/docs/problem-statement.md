# Problem Statement

## The scenario

An online bank keeps customers signed in with a session cookie. Whoever presents a valid session id is treated as that customer, including for payments.

## The naive version

Use a predictable or long-lived id, keep the same id when the customer signs in, never expire it, and sign out by deleting the cookie in the browser only.

## What this project must deliver

- Session ids of 128 random bits, stored only as their SHA-256 and compared in constant time.
- A new id at sign-in and at step-up, so session fixation fails.
- Idle and absolute timeouts, and the password again for payments.
- Sign-out on the server, sign-out everywhere, sessions ended by a password change, and a limit on sessions.
- A session cookie with Secure, HttpOnly, SameSite=Strict and the __Host- prefix, checked on a real local server.
